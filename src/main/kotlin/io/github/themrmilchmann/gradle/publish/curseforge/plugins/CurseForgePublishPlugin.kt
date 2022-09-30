/*
 * Copyright (c) 2021-2022 Leon Linhart
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
import org.gradle.api.internal.file.*
import org.gradle.api.invocation.*
import org.gradle.api.model.*
import org.gradle.api.plugins.*
import org.gradle.api.publish.*
import org.gradle.api.publish.plugins.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import java.util.*
import javax.inject.*

public class CurseForgePublishPlugin @Inject constructor(
    private val objectFactory: ObjectFactory,
    private val fileResolver: FileResolver
) : Plugin<Project> {

    internal companion object {

        private val LOGGER = LogManager.getLogger(CurseForgePublishPlugin::class.java)

        lateinit var gradle: Gradle
            private set

    }

    override fun apply(target: Project): Unit = applyTo(target) {
        pluginManager.apply(PublishingPlugin::class)

        CurseForgePublishPlugin.gradle = gradle

        extensions.configure<PublishingExtension> {
            repositories {
                this as ExtensionAware

                extensions.create("curseForge", CurseForgeRepositoryExtension::class, repositories)
            }

            publications.registerFactory(CurseForgePublication::class.java, CurseForgePublicationFactory(fileResolver))
            realizePublishingTasksLater(target)
        }

        configureForgeGradleIntegration()
        configureJavaIntegration()
    }

    private fun PublishingExtension.realizePublishingTasksLater(project: Project) {
        val curseForgePublications = publications.withType<CurseForgePublicationInternal>()
        val tasks = project.tasks

        val publishLifecycleTask = tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)
        val repositories = repositories.withType<CurseForgeArtifactRepository>()

        repositories.all repository@{
            tasks.register(publishAllToSingleRepoTaskName(this@repository)) {
                description = "Publishes all CurseForge publications produced by this project to the ${this@repository.name} repository."
                group = PublishingPlugin.PUBLISH_TASK_GROUP
            }
        }

        curseForgePublications.all {
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
        val generatorTask = tasks.register<GeneratePublicationMetadata>("generateMetadataFilesFor${publication.name.capitalize(Locale.ROOT)}Publication") {
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

            tasks.register<PublishToCurseForgeRepository>(publishTaskName) {
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

    private fun Project.configureForgeGradleIntegration() {
        pluginManager.withPlugin("net.minecraftforge.gradle") {
            extensions.configure<PublishingExtension> {
                publications.withType<CurseForgePublication> {
                    includeGameVersions { type, version -> type == "modloader" && version == "forge" }

                    afterEvaluate {
                        val mcVersion = this@configureForgeGradleIntegration.extensions.extraProperties["MC_VERSION"] as String
                        val matchGroups = """^([0-9]+)\.([0-9]+)(?:\.([0-9]+))?""".toRegex().matchEntire(mcVersion)?.groupValues

                        if (matchGroups == null) {
                            LOGGER.warn("Failed to parse Minecraft version string '$mcVersion'. The CurseForge publication cannot infer the required Minecraft version.")
                            return@afterEvaluate
                        }

                        val mcDependencySlug = "minecraft-${matchGroups[1]}-${matchGroups[2]}"
                        val mcVersionSlug = "${matchGroups[1]}-${matchGroups[2]}-${matchGroups[3]}"

                        LOGGER.debug("Inferred CurseForge Minecraft dependency: type='$mcDependencySlug', version='$mcVersionSlug'")
                        includeGameVersions { type, version -> type == mcDependencySlug && version == mcVersionSlug }
                    }
                }
            }

            afterEvaluate {
                tasks.withType<AbstractPublishToCurseForge> {
                    dependsOn(tasks["reobfJar"])
                }
            }
        }
    }

    private fun Project.configureJavaIntegration() {
        pluginManager.withPlugin("java") {
            extensions.configure<PublishingExtension> {
                publications.withType<CurseForgePublication> {
                    val jar = tasks.named(JavaPlugin.JAR_TASK_NAME)
                    artifact(jar)

                    afterEvaluate {
                        val java = project.extensions.getByType(JavaPluginExtension::class)
                        val targetVersion = java.targetCompatibility.majorVersion

                        includeGameVersions { type, version -> type == "java" && version == "java-$targetVersion" }
                    }
                }
            }
        }
    }

    private inner class CurseForgePublicationFactory(
        private val fileResolver: FileResolver
    ) : NamedDomainObjectFactory<CurseForgePublication> {

        override fun create(name: String): CurseForgePublication {
            return objectFactory.newInstance<DefaultCurseForgePublication>(name, CurseForgeArtifactNotationParser(fileResolver))
        }

    }

}