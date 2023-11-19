/*
 * Copyright (c) 2022-2023 Leon Linhart
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.github.themrmilchmann.gradle.publish.curseforge.plugins

import io.github.themrmilchmann.gradle.publish.curseforge.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.publication.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.utils.*
import io.github.themrmilchmann.gradle.publish.curseforge.tasks.*
import org.apache.log4j.LogManager
import org.gradle.api.*
import org.gradle.api.invocation.*
import org.gradle.api.model.*
import org.gradle.api.plugins.*
import org.gradle.api.provider.Provider
import org.gradle.api.publish.*
import org.gradle.api.publish.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.JavaCompile
import java.lang.IllegalStateException
import java.util.*
import javax.inject.Inject

public class CurseForgePublishPlugin @Inject constructor(
    private val objectFactory: ObjectFactory
) : Plugin<Project> {

    internal companion object {

        private val LOGGER = LogManager.getLogger(CurseForgePublishPlugin::class.java)

        lateinit var gradle: Gradle
            private set

    }

    override fun apply(target: Project): Unit = applyTo(target) {
        pluginManager.apply(PublishingPlugin::class.java)

        CurseForgePublishPlugin.gradle = gradle

        val defaultGameVersions = objects.setProperty(GameVersion::class.java)
        val defaultJavaVersions = objects.setProperty(JavaVersion::class.java)

        val publishing = extensions.getByType(PublishingExtension::class.java)
        applyTo(publishing) {
            repositories {
                this as ExtensionAware
                extensions.create("curseForge", CurseForgeRepositoryExtension::class.java, repositories)
            }

            publications.registerFactory(CurseForgePublication::class.java, CurseForgePublicationFactory(objectFactory))

            realizePublishingTasksLater(
                project = target,
                defaultGameVersions = defaultGameVersions,
                defaultJavaVersions = defaultJavaVersions
            )
        }

        var isUsingForgeGradle = false
        var isUsingLoom = false

        project.pluginManager.withPlugin("fabric-loom") {
            if (isUsingForgeGradle) throw IllegalStateException("Cannot apply both ForgeGradle and Loom")
            isUsingLoom = true

            publishing.publications.withType(CurseForgePublication::class.java).configureEach {
                artifact(tasks.named("remapJar"))
            }

            defaultGameVersions.set(provider {
                val gameVersions = mutableSetOf<GameVersion>()
                gameVersions += GameVersion("modloader", "fabric")

                val minecraftConfiguration = configurations.findByName("minecraft")
                if (minecraftConfiguration == null) {
                    LOGGER.warn("Fabric Loom Gradle Plugin was detected but 'minecraft' configuration cannot be found.")
                    return@provider gameVersions
                }

                val dependency = minecraftConfiguration
                    .dependencies
                    .find { it.group == "com.mojang" && it.name == "minecraft" }

                if (dependency == null) {
                    LOGGER.warn("Cannot find Minecraft dependency: ('com.mojang:minecraft')'")
                    return@provider gameVersions
                }

                val mcVersion = dependency.version ?: error("")
                val mcGameVersion = inferMinecraftGameVersion(mcVersion)

                if (mcGameVersion != null) {
                    gameVersions += mcGameVersion
                } else {
                    LOGGER.warn("Could not infer Minecraft game version from dependency version: $mcVersion")
                }

                gameVersions
            })
        }

        pluginManager.withPlugin("net.minecraftforge.gradle") {
            if (isUsingLoom) throw IllegalStateException("Cannot apply both ForgeGradle and Loom")
            isUsingForgeGradle = true

            publishing.publications.withType(CurseForgePublication::class.java).configureEach {
                artifact(tasks.named(JavaPlugin.JAR_TASK_NAME))
            }

            defaultGameVersions.set(provider {
                val gameVersions = mutableSetOf<GameVersion>()
                gameVersions += GameVersion("modloader", "forge")

                val mcVersion = project.extensions.extraProperties["MC_VERSION"] as String
                inferMinecraftGameVersion(mcVersion)

                gameVersions
            })

            tasks.withType(AbstractPublishToCurseForge::class.java).configureEach {
                dependsOn(tasks.named("reobfJar"))
            }
        }

        configureJavaIntegration()
    }

    private fun PublishingExtension.realizePublishingTasksLater(
        project: Project,
        defaultGameVersions: Provider<Set<GameVersion>>,
        defaultJavaVersions: Provider<Set<JavaVersion>>
    ) {
        val curseForgePublications = publications.withType(CurseForgePublicationInternal::class.java)
        val tasks = project.tasks

        val publishLifecycleTask = tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)
        val repositories = repositories.withType(CurseForgeArtifactRepository::class.java)

        repositories.all repository@{
            tasks.register(publishAllToSingleRepoTaskName(this@repository)) {
                description = "Publishes all CurseForge publications produced by this project to the ${this@repository.name} repository."
                group = PublishingPlugin.PUBLISH_TASK_GROUP
            }
        }

        curseForgePublications.all {
            gameVersions.convention(defaultGameVersions)
            javaVersions.convention(defaultJavaVersions)

            createGenerateMetadataTask(tasks, this)
            createPublishTasksForEachCurseForgeRepo(tasks, publishLifecycleTask, this, repositories)
        }
    }

    private fun publishAllToSingleRepoTaskName(repository: CurseForgeArtifactRepository): String =
        "publishAllPublicationsTo${repository.name.capitalize(Locale.ROOT)}Repository"

    private fun createGenerateMetadataTask(
        tasks: TaskContainer,
        publication: CurseForgePublicationInternal
    ) {
        val generatorTask = tasks.register("generateMetadataFilesFor${publication.name.capitalize(Locale.ROOT)}Publication", GeneratePublicationMetadata::class.java) {
            description = "Generates CurseForge metadata for publication '${publication.name}'."
            group = PublishingPlugin.PUBLISH_TASK_GROUP

            this.publication.set(publication)
        }

        publication.publicationMetadataGenerator = generatorTask
    }

    private fun createPublishTasksForEachCurseForgeRepo(
        tasks: TaskContainer,
        publishLifecycleTask: TaskProvider<Task>,
        publication: CurseForgePublicationInternal,
        repositories: NamedDomainObjectCollection<CurseForgeArtifactRepository>
    ) {
        val publicationName = publication.name

        repositories.all repository@{
            val repositoryName = name
            val publishTaskName = "publish${publicationName.capitalize(Locale.ROOT)}PublicationTo${repositoryName.capitalize(Locale.ROOT)}Repository"

            tasks.register(publishTaskName, PublishToCurseForgeRepository::class.java) {
                dependsOn(publication.publicationMetadataGenerator)

                description = "Publishes CurseForge publication '$publicationName' to CurseForge repository '$repositoryName'"
                group = PublishingPlugin.PUBLISH_TASK_GROUP

                repository = this@repository
                this.publication = publication
            }

            publishLifecycleTask.configure { dependsOn(publishTaskName) }
            tasks.named(publishAllToSingleRepoTaskName(this@repository)) { dependsOn(publishTaskName) }
        }
    }

    private fun Project.configureJavaIntegration() {
        pluginManager.withPlugin("java") {
            extensions.configure(PublishingExtension::class.java) {
                publications.withType(CurseForgePublication::class.java).configureEach {
                    val compileJava = tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaCompile::class.java).get()
                    val targetVersionProvider = compileJava.options.release.map(Int::toString)
                        .orElse(compileJava.targetCompatibility)
                        .map {
                            // Normalize (e.g 1.8 => 8)
                            val version = JavaVersion.toVersion(it)
                            LOGGER.debug("Inferred CurseForge Java dependency: version='java-${version.majorVersion}'")
                            version
                        }

                    javaVersions.convention(targetVersionProvider.map { setOf(it) })
                }
            }
        }
    }

    private fun inferMinecraftGameVersion(version: String): GameVersion? {
        val matchGroups = """^([0-9]+)\.([0-9]+)(?:\.([0-9]+))?""".toRegex().matchEntire(version)?.groupValues

        if (matchGroups == null) {
            LOGGER.warn("Failed to parse Minecraft version string '$version'. The CurseForge publication cannot infer the required Minecraft version.")
            return null
        }

        val mcDependencySlug = "minecraft-${matchGroups[1]}-${matchGroups[2]}"
        val mcVersionSlug = "${matchGroups[1]}-${matchGroups[2]}-${matchGroups[3]}"

        LOGGER.debug("Inferred CurseForge Minecraft dependency: type='$mcDependencySlug', version='$mcVersionSlug'")
        return GameVersion(mcDependencySlug, mcVersionSlug)
    }

    private inner class CurseForgePublicationFactory(
        private val objectFactory: ObjectFactory
    ) : NamedDomainObjectFactory<CurseForgePublication> {

        override fun create(name: String): CurseForgePublication {
            val parser = objectFactory.newInstance(CurseForgeArtifactNotationParser::class.java)
            return objectFactory.newInstance(DefaultCurseForgePublication::class.java, name, parser)
        }

    }

}