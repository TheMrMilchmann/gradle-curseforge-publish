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
    alias(buildDeps.plugins.binary.compatibility.validator)
    alias(buildDeps.plugins.gradle.shadow)
    alias(buildDeps.plugins.gradle.toolchain.switches)
    alias(buildDeps.plugins.java.gradle.plugin)
    alias(buildDeps.plugins.kotlin.jvm)
    alias(buildDeps.plugins.kotlin.plugin.samwithreceiver)
    alias(buildDeps.plugins.kotlin.plugin.serialization)
    alias(buildDeps.plugins.plugin.publish)
    id("io.github.themrmilchmann.maven-publish-conventions")
    `jvm-test-suite`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }

    withJavadocJar()
    withSourcesJar()
}

kotlin {
    explicitApi()

    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_1_8
        languageVersion = KotlinVersion.KOTLIN_1_8

        jvmTarget = JvmTarget.JVM_17

        freeCompilerArgs.add("-Xjdk-release=17")
    }

    target {
        val mainCompilation = compilations.named("main") {
            compileJavaTaskProvider.configure {
                options.release.set(17)
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
                implementation(platform(buildDeps.junit.bom))
                implementation(buildDeps.junit.jupiter.api)
                implementation(buildDeps.junit.jupiter.params)
                runtimeOnly(buildDeps.junit.jupiter.engine)
            }
        }

        val test = named<JvmTestSuite>("test") {
            dependencies {
                implementation(buildDeps.ktor.client.mock)
            }
        }

        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
                implementation(gradleTestKit())
                implementation(buildDeps.fabric.loom)
                implementation(buildDeps.forgegradle)
                implementation(buildDeps.neogradle) {
                    exclude(group = "org.codehaus.groovy")
                }
                implementation(buildDeps.moddevgradle)
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
                implementation(buildDeps.ktor.server.netty)
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
        options.release = 17
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

dependencies {
    compileOnlyApi(kotlin("stdlib"))

    implementation(buildDeps.kotlinx.serialization.json) {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation(buildDeps.ktor.client.apache) {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation(buildDeps.ktor.client.content.negotiation) {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation(buildDeps.ktor.serialization.kotlinx.json) {
        exclude(group = "org.jetbrains.kotlin")
    }
}