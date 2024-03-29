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
import io.github.themrmilchmann.gradle.toolchainswitches.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.gradle.plugin.functional.test)
    alias(libs.plugins.gradle.plugin.unit.test)
    alias(libs.plugins.gradle.shadow)
    alias(libs.plugins.gradle.toolchain.switches)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.samwithreceiver)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.plugin.publish)
    id("io.github.themrmilchmann.maven-publish-conventions")
}

sourceSets {
    register("integrationTest") {
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }

    withJavadocJar()
    withSourcesJar()
}

kotlin {
    explicitApi()

    target {
        compilations.all {
            compilerOptions.configure {
                apiVersion = KotlinVersion.KOTLIN_1_8
                languageVersion = KotlinVersion.KOTLIN_1_8
            }
        }

        compilations.named("main").configure {
            compilerOptions.configure {
                @Suppress("DEPRECATION")
                apiVersion = KotlinVersion.KOTLIN_1_4

                /*
                 * 1.4 is deprecated, but we need it to stay compatible with old
                 * Gradle versions anyway. Thus, we suppress the compiler's
                 * warning.
                 */
                freeCompilerArgs.add("-Xsuppress-version-warnings")
            }
        }

        compilations.named("integrationTest") {
            associateWith(compilations.getByName("main"))
        }
    }
}

gradlePlugin {
    compatibility {
        minimumGradleVersion = "7.6"
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

tasks {
    withType<JavaCompile>().configureEach {
        options.release = 8
    }

    withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    jar {
        enabled = false
    }

    shadowJar {
        isEnableRelocation = true
        relocationPrefix = "io.github.themrmilchmann.gradle.publish.curseforge.internal.shadow"

        archiveClassifier = null as String?
    }

    val test by getting

    val integrationTest = register<Test>("integrationTest") {
        group = JavaBasePlugin.VERIFICATION_GROUP
        description = "Runs the integration tests."

        // It is not necessary to run integration tests after unit tests, but it's generally a good practice.
        shouldRunAfter(test)

        testClassesDirs = sourceSets.named("integrationTest").get().output.classesDirs
        classpath = sourceSets.named("integrationTest").get().runtimeClasspath
    }

    withType<Test>().configureEach {
        dependsOn(shadowJar)

        useJUnitPlatform()

        @OptIn(ExperimentalToolchainSwitchesApi::class)
        javaLauncher.set(inferLauncher(default = project.javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(17)
        }))
    }

    check {
        dependsOn(integrationTest)
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

// TODO Figure out a clean way to do this
afterEvaluate {
    configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME) {
        dependencies.remove(project.dependencies.gradleApi())
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

    testImplementation(kotlin("stdlib"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(libs.ktor.client.mock)

    functionalTestImplementation(kotlin("stdlib"))
    functionalTestImplementation(platform(libs.junit.bom))
    functionalTestImplementation(libs.junit.jupiter.api)
    functionalTestImplementation(libs.junit.jupiter.params)
    functionalTestRuntimeOnly(libs.junit.jupiter.engine)

    functionalTestImplementation(libs.ktor.server.netty)

    "integrationTestImplementation"(kotlin("stdlib"))
    "integrationTestImplementation"(platform(libs.junit.bom))
    "integrationTestImplementation"(libs.junit.jupiter.api)
    "integrationTestImplementation"(libs.junit.jupiter.params)
    "integrationTestRuntimeOnly"(libs.junit.jupiter.engine)
    "integrationTestRuntimeOnly"(gradleTestKit())

    "integrationTestImplementation"(libs.fabric.loom)
    "integrationTestImplementation"(libs.forgegradle)
    "integrationTestImplementation"(libs.neogradle)
}