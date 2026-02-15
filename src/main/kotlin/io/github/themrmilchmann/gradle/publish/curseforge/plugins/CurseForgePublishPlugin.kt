/*
 * Copyright (c) 2022-2024 Leon Linhart
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
import io.github.themrmilchmann.gradle.publish.curseforge.internal.interop.moddevgradle.deriveMinecraftVersionFromModDevGradleExtension
import io.github.themrmilchmann.gradle.publish.curseforge.internal.utils.*
import io.github.themrmilchmann.gradle.publish.curseforge.tasks.*
import org.gradle.api.*
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.plugins.*
import org.gradle.api.provider.Provider
import org.gradle.api.publish.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.util.GradleVersion
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
        if (GradleVersion.current() < GradleVersion.version("9.0.0")) {
            throw IllegalStateException("This plugin requires Gradle 9.0.0 or later")
        }

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
            val publishPublicationToCurseForgeTask = tasks.register("publish${name.replaceFirstChar(Char::uppercase)}PublicationToCurseForge", PublishToCurseForgeRepository::class.java) {
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
        configureNeoForgeModDevGradleIntegration(cfExtension.publications)
        configureNeoGradleIntegration(cfExtension.publications)
    }

    private fun Project.configureFabricLoomIntegration(publications: CurseForgePublicationContainer) {
        val isEnabled = providers.gradleProperty("gradle-curseforge-publish.interop.fabric-loom").map(String::toBoolean).getOrElse(true)

        if (!isEnabled) {
            LOGGER.debug("Fabric Loom integration is disabled")
            return
        }

        LOGGER.debug("Fabric Loom integration is enabled")

        pluginManager.withPlugin("fabric-loom") {
            LOGGER.debug("Fabric Loom plugin detected")

            val defaultGameVersions: Provider<Set<GameVersion>> = provider {
                val gameVersions = mutableSetOf<GameVersion>()
                gameVersions += GameVersion(type = "modloader", version = "fabric")

                val mcGameVersion = inferGameVersionFromDependency(
                    configurations,
                    configurationName = "minecraft",
                    integration = "FabricLoom",
                    group = "com.mojang",
                    name = "minecraft",
                    extractVersion = ::extractMinecraftVersionFromFabricLoomMinecraftDependencyVersion
                )

                if (mcGameVersion != null) gameVersions += mcGameVersion
                gameVersions.toSet()
            }

            val publicationName = providers.gradleProperty("gradle-curseforge-publish.interop.fabric-loom.publication-name").getOrElse("fabric")
            publications.register(publicationName) {
                gameVersions.convention(defaultGameVersions)

                artifacts.register("main") {
                    from(tasks.named("remapJar"))
                }
            }
        }
    }

    private fun Project.configureNeoForgeModDevGradleIntegration(publications: CurseForgePublicationContainer) {
        val isEnabled = providers.gradleProperty("gradle-curseforge-publish.interop.neoforged").map(String::toBoolean).getOrElse(true)

        if (!isEnabled) {
            LOGGER.debug("NeoForge ModDevGradle integration is disabled")
            return
        }

        LOGGER.debug("NeoForge ModDevGradle integration is enabled")

        pluginManager.withPlugin("net.neoforged.moddev") {
            val defaultGameVersions: Provider<Set<GameVersion>> = provider {
                LOGGER.debug("NeoForge ModDevGradle plugin detected")

                val gameVersions = mutableSetOf<GameVersion>()
                gameVersions += GameVersion(type = "modloader", version = "neoforge")

                val minecraftVersion = deriveMinecraftVersionFromModDevGradleExtension()
                if (minecraftVersion != null) {
                    val (one, major, minor) = minecraftVersion

                    val mcGameVersion = GameVersion(type = "minecraft-$one-$major", version = "$one-$major-$minor")
                    gameVersions += mcGameVersion

                    LOGGER.debug("Inferred CurseForge Minecraft dependency: type='${mcGameVersion.type}', version='${mcGameVersion.version}'")
                } else {
                    LOGGER.warn("[ModDevGradle] Could not infer Minecraft version from dependency version '$minecraftVersion'")
                }

                gameVersions.toSet()
            }

            val publicationName = providers.gradleProperty("gradle-curseforge-publish.interop.neoforged.publication-name").getOrElse("neoForge")
            publications.register(publicationName) {
                gameVersions.convention(defaultGameVersions)

                artifacts.register("main") {
                    from(tasks.named("jar"))
                }
            }
        }
    }

    private fun Project.configureNeoGradleIntegration(publications: CurseForgePublicationContainer) {
        val isEnabled = providers.gradleProperty("gradle-curseforge-publish.interop.neogradle").map(String::toBoolean).getOrElse(true)

        if (!isEnabled) {
            LOGGER.debug("NeoGradle integration is disabled")
            return
        }

        LOGGER.debug("NeoGradle integration is enabled")

        pluginManager.withPlugin("net.neoforged.gradle.userdev") {
            val defaultGameVersions: Provider<Set<GameVersion>> = provider {
                LOGGER.debug("NeoGradle plugin detected")

                val gameVersions = mutableSetOf<GameVersion>()
                gameVersions += GameVersion(type = "modloader", version = "neoforge")

                val mcGameVersion = inferGameVersionFromDependency(
                    configurations,
                    JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME,
                    integration = "NeoGradle",
                    group = "ng_dummy_ng.net.neoforged",
                    name = "neoforge",
                    extractVersion = ::extractMinecraftVersionFromNeoForgeVersion
                )

                if (mcGameVersion != null) gameVersions += mcGameVersion
                gameVersions.toSet()
            }

            val publicationName = providers.gradleProperty("gradle-curseforge-publish.interop.neogradle.publication-name").getOrElse("neoForge")
            publications.register(publicationName) {
                gameVersions.convention(defaultGameVersions)

                artifacts.register("main") {
                    from(tasks.named("jar"))
                }
            }
        }
    }

    private fun inferGameVersionFromDependency(
        configurations: ConfigurationContainer,
        configurationName: String,
        integration: String,
        group: String,
        name: String,
        extractVersion: (String) -> MinecraftVersion?
    ): GameVersion? {
        val configuration = configurations.findByName(configurationName) ?: let {
            LOGGER.warn("[$integration] Configuration '$configurationName' could not be found")
            return null
        }

        val dependency = configuration.allDependencies.find { it.group == group && it.name == name } ?: let {
            LOGGER.warn("[$integration] Could not find Minecraft dependency '$group:$name' in configuration '$configurationName'")
            return null
        }

        val dependencyVersion = dependency.version ?: let {
            LOGGER.warn("[$integration] Found Minecraft dependency does not have a declared version")
            return null
        }

        // https://help.minecraft.net/hc/en-us/articles/9971900758413-Major-Minor-Versions-in-Minecraft-Java-Edition
        val (one, major, minor) = extractVersion(dependencyVersion) ?: let {
            LOGGER.warn("[$integration] Could not infer Minecraft version from dependency version '$dependencyVersion'")
            return null
        }

        val mcGameVersion = GameVersion(type = "minecraft-$one-$major", version = "$one-$major-$minor")
        LOGGER.debug("Inferred CurseForge Minecraft dependency: type='${mcGameVersion.type}', version='${mcGameVersion.version}'")

        return mcGameVersion
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