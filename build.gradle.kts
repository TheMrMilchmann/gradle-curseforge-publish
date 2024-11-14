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
import io.github.themrmilchmann.gradle.toolchainswitches.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.gradle.shadow)
    alias(libs.plugins.gradle.toolchain.switches)
    alias(libs.plugins.java.gradle.plugin)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.samwithreceiver)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.plugin.publish)
    id("io.github.themrmilchmann.maven-publish-conventions")
    `jvm-test-suite`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }

    withJavadocJar()
    withSourcesJar()
}

kotlin {
    explicitApi()

    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_1_8
        languageVersion = KotlinVersion.KOTLIN_1_8

        jvmTarget = JvmTarget.JVM_1_8

        freeCompilerArgs.add("-Xjdk-release=1.8")
    }

    target {
        val mainCompilation = compilations.named("main") {
            compileJavaTaskProvider.configure {
                options.release.set(8)
            }
        }

        compilations.configureEach {
            if (name == "integrationTest") {
                associateWith(mainCompilation.get())
            }
        }
    }
}

gradlePlugin {
    compatibility {
        minimumGradleVersion = "8.0"
    }

    website = "https://github.com/TheMrMilchmann/gradle-curseforge-publish"
    vcsUrl = "https://github.com/TheMrMilchmann/gradle-curseforge-publish.git"

    plugins {
        register("curseForgePublish") {
            id = "io.github.themrmilchmann.curseforge-publish"
            displayName = "CurseForge Gradle Publish Plugin"
            description = "A Gradle plugin that provides support for publishing artifacts to CurseForge."
            tags.addAll("curseforge", "minecraft", "publishing")

            implementationClass = "io.github.themrmilchmann.gradle.publish.curseforge.plugins.CurseForgePublishPlugin"
        }
    }
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter()

            dependencies {
                implementation(platform(libs.junit.bom))
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit.jupiter.params)
                runtimeOnly(libs.junit.jupiter.engine)
            }
        }

        val test = named<JvmTestSuite>("test") {
            dependencies {
                implementation(libs.ktor.client.mock)
            }
        }

        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
                implementation(gradleTestKit())
                implementation(libs.fabric.loom)
                implementation(libs.forgegradle)
                implementation(libs.neogradle) {
                    exclude(group = "org.codehaus.groovy")
                }
                implementation(libs.moddevgradle)
            }

            targets.configureEach {
                testTask.configure {
                    shouldRunAfter(test)
                }
            }
        }

        register<JvmTestSuite>("functionalTest") {
            dependencies {
                implementation(project())
                implementation(gradleTestKit())
                implementation(libs.ktor.server.netty)
                runtimeOnly(layout.files(tasks.named("pluginUnderTestMetadata")))
            }

            targets.configureEach {
                testTask.configure {
                    shouldRunAfter(test)
                }
            }
        }
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release = 8
    }

    jar {
        enabled = false
    }

    shadowJar {
        isEnableRelocation = true
        relocationPrefix = "io.github.themrmilchmann.gradle.publish.curseforge.internal.shadow"

        archiveClassifier = null as String?
    }

    withType<Test>().configureEach {
        dependsOn(shadowJar)

        @OptIn(ExperimentalToolchainSwitchesApi::class)
        javaLauncher.set(inferLauncher(default = project.javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(17)
        }))
    }

    @Suppress("UnstableApiUsage")
    check {
        dependsOn(testing.suites.named("functionalTest"))
        dependsOn(testing.suites.named("integrationTest"))
    }

    validatePlugins {
        enableStricterValidation = true
    }
}

artifacts {
    runtimeOnly(tasks.shadowJar)
    archives(tasks.shadowJar)
}

val emptyJar = tasks.register<Jar>("emptyJar") {
    destinationDirectory = layout.buildDirectory.dir("emptyJar")
    archiveBaseName = "io.github.themrmilchmann.curseforge-publish.gradle.plugin"
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        if (name == "curseForgePublishPluginMarkerMaven") {
            artifact(emptyJar)
            artifact(emptyJar) { classifier = "javadoc" }
            artifact(emptyJar) { classifier = "sources" }
        }

        pom {
            name = "CurseForge Gradle Publish Plugin"
            description = "A Gradle plugin that provides support for publishing artifacts to CurseForge."
        }
    }
}

configurations {
    named("integrationTestCompileOnly").configure {
        extendsFrom(compileOnlyApi.get())
    }
    named("integrationTestImplementation").configure {
        extendsFrom(implementation.get())
    }
    named("integrationTestRuntimeOnly").configure {
        extendsFrom(runtimeOnly.get())
    }
}

repositories {
    gradlePluginPortal()

    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net")
    }

    maven {
        name = "MinecraftForge"
        url = uri("https://maven.minecraftforge.net")
    }

    maven {
        name = "NeoForged"
        url = uri("https://maven.neoforged.net/releases")
    }
}

dependencies {
    compileOnlyApi(kotlin("stdlib"))

    compileOnly(libs.moddevgradle)

    implementation(libs.kotlinx.serialization.json) {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation(libs.ktor.client.apache) {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation(libs.ktor.client.content.negotiation) {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation(libs.ktor.serialization.kotlinx.json) {
        exclude(group = "org.jetbrains.kotlin")
    }
}