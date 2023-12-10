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
import io.github.themrmilchmann.gradle.publish.curseforge.internal.DefaultCurseForgePublicationContainer
import io.github.themrmilchmann.gradle.publish.curseforge.internal.utils.*
import io.github.themrmilchmann.gradle.publish.curseforge.tasks.*
import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.provider.Provider
import org.gradle.api.publish.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.JavaCompile
import org.slf4j.LoggerFactory
import javax.inject.Inject

/**
 * Provides support for publishing artifacts to CurseForge.
 *
 * @since   0.6.0
 *
 * @author  Leon Linhart
 */
public class CurseForgePublishPlugin @Inject private constructor() : Plugin<Project> {

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(CurseForgePublishPlugin::class.java)

    }

    override fun apply(target: Project): Unit = applyTo(target) {
        val cfExtension = extensions.create(
            CurseForgePublishingExtension.NAME,
            CurseForgePublishingExtension::class.java,
            objects.newInstance(DefaultCurseForgePublicationContainer::class.java)
        )

        val publishToCurseForgeTask = tasks.register("publishToCurseForge") {
            description = "Publishes all CurseForge publications produced by this project."
            group = PublishingPlugin.PUBLISH_TASK_GROUP
        }

        cfExtension.publications.all publication@{
            val publishPublicationToCurseForgeTask = tasks.register("publish${name.capitalize()}PublicationToCurseForge", PublishToCurseForgeRepository::class.java) {
                description = "Publishes CurseForge publication '$name'"
                group = PublishingPlugin.PUBLISH_TASK_GROUP

                publication = this@publication

                apiToken.convention(cfExtension.apiToken)
            }

            publishToCurseForgeTask.configure {
                dependsOn(publishPublicationToCurseForgeTask)
            }
        }

        configureJavaIntegration(cfExtension.publications)
        configurePublishingIntegration(publishToCurseForgeTask)

        configureFabricLoomIntegration(cfExtension.publications)
        configureForgeGradleIntegration(cfExtension.publications)
        configureNeoGradleIntegration(cfExtension.publications)
    }

    private fun Project.configureFabricLoomIntegration(publications: CurseForgePublicationContainer) {
        pluginManager.withPlugin("fabric-loom") {
            val defaultGameVersions: Provider<Set<GameVersion>> = provider {
                val gameVersions = mutableSetOf<GameVersion>()
                gameVersions += GameVersion(type = "modloader", version = "fabric")

                val minecraftConfiguration = configurations.findByName("minecraft")
                if (minecraftConfiguration == null) {
                    LOGGER.warn("Fabric Loom Gradle Plugin was detected but 'minecraft' configuration cannot be found.")
                    return@provider gameVersions.toSet()
                }

                val dependency = minecraftConfiguration.dependencies.find { it.group == "com.mojang" && it.name == "minecraft" }
                if (dependency == null) {
                    LOGGER.warn("Cannot find Minecraft dependency: ('com.mojang:minecraft')'")
                    return@provider gameVersions.toSet()
                }

                val mcVersion = dependency.version
                if (mcVersion == null) {
                    LOGGER.warn("Could not infer Minecraft game version from dependency version: $mcVersion")
                    return@provider gameVersions.toSet()
                }

                val mcGameVersion = inferMinecraftGameVersion(mcVersion) // TODO
                if (mcGameVersion == null) {
                    LOGGER.warn("Could not infer Minecraft game version from dependency version: $mcVersion")
                    return@provider gameVersions.toSet()
                }

                LOGGER.debug("Inferred CurseForge Minecraft dependency: type='${mcGameVersion.type}', version='${mcGameVersion.version}'")
                gameVersions += mcGameVersion
                gameVersions.toSet()
            }

            publications.register("fabric") {
                gameVersions.convention(defaultGameVersions)

                artifacts.register("main") {
                    from(tasks.named("remapJar"))
                }
            }
        }
    }

    private fun Project.configureForgeGradleIntegration(publications: CurseForgePublicationContainer) {
        pluginManager.withPlugin("net.minecraftforge.gradle") {
            val defaultGameVersions: Provider<Set<GameVersion>> = provider {
                val gameVersions = mutableSetOf<GameVersion>()
                gameVersions += GameVersion(type = "modloader", version = "forge")

                val mcVersion = project.extensions.extraProperties["MC_VERSION"] as String
                val mcGameVersion = inferMinecraftGameVersion(mcVersion)
                if (mcGameVersion == null) {
                    LOGGER.warn("Could not infer Minecraft game version from dependency version: $mcVersion")
                    return@provider gameVersions.toSet()
                }

                LOGGER.debug("Inferred CurseForge Minecraft dependency: type='${mcGameVersion.type}', version='${mcGameVersion.version}'")
                gameVersions += mcGameVersion
                gameVersions.toSet()
            }

            publications.register("minecraftForge") {
                gameVersions.convention(defaultGameVersions)

                artifacts.register("main") {
                    from(tasks.named("jar"))
                }
            }
        }
    }

    private fun Project.configureNeoGradleIntegration(publications: CurseForgePublicationContainer) {
        pluginManager.withPlugin("net.neoforged.gradle.userdev") {
            val defaultGameVersions: Provider<Set<GameVersion>> = provider {
                val gameVersions = mutableSetOf<GameVersion>()
                gameVersions += GameVersion(type = "modloader", version = "neoforge")

                val implementationConfiguration = configurations.findByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
                if (implementationConfiguration == null) {
                    LOGGER.warn("Fabric Loom Gradle Plugin was detected but 'minecraft' configuration cannot be found.")
                    return@provider gameVersions.toSet()
                }

                val dependency = implementationConfiguration.dependencies.find { it.group == "net.neoforged" && it.name == "neoforge" }
                if (dependency == null) {
                    LOGGER.warn("Cannot find Minecraft dependency: ('net.neoforged:neoforge')'")
                    return@provider gameVersions.toSet()
                }

                val neoforgeVersion = dependency.version
                if (neoforgeVersion == null) {
                    LOGGER.warn("Could not infer Minecraft game version from dependency version: $neoforgeVersion")
                    return@provider gameVersions.toSet()
                }

                val mcGameVersion = inferMinecraftGameVersionFromNeoForgeDependency(neoforgeVersion) // TODO
                if (mcGameVersion == null) {
                    LOGGER.warn("Could not infer Minecraft game version from dependency version: $neoforgeVersion")
                    return@provider gameVersions.toSet()
                }

                LOGGER.debug("Inferred CurseForge Minecraft dependency: type='${mcGameVersion.type}', version='${mcGameVersion.version}'")
                gameVersions += mcGameVersion
                gameVersions.toSet()
            }

            publications.register("neoForge") {
                gameVersions.convention(defaultGameVersions)

                artifacts.register("main") {
                    from(tasks.named("jar"))
                }
            }
        }
    }

    private fun Project.configureJavaIntegration(publications: CurseForgePublicationContainer) {
        pluginManager.withPlugin("java-base") {
            val defaultJavaVersions: Provider<Set<JavaVersion>> = provider {
                val javaVersions = mutableSetOf<JavaVersion>()

                val compileJavaTask = tasks.getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME) as JavaCompile

                val javaVersion = compileJavaTask.options.release
                    .map(Int::toString)
                    .orElse(compileJavaTask.targetCompatibility)
                    .map(JavaVersion::toVersion)
                    .get()

                javaVersions += javaVersion
                javaVersions.toSet()
            }

            publications.configureEach {
                javaVersions.convention(defaultJavaVersions)
            }
        }
    }

    private fun Project.configurePublishingIntegration(publishToCurseForgeTask: TaskProvider<*>) {
        pluginManager.apply(PublishingPlugin::class.java)

        tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME) {
            dependsOn(publishToCurseForgeTask)
            dependsOn(tasks.withType(PublishToCurseForgeRepository::class.java))
        }
    }

}