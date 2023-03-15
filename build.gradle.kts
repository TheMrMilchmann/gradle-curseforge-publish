/*
 * Copyright (c) 2022 Leon Linhart
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
import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.*

@Suppress("DSL_SCOPE_VIOLATION") // See https://github.com/gradle/gradle/issues/22797
plugins {
    groovy
    alias(libs.plugins.gradle.shadow)
    alias(libs.plugins.gradle.toolchain.switches)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.samwithreceiver)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.plugin.publish)
    id("io.github.themrmilchmann.maven-publish-conventions")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    withJavadocJar()
    withSourcesJar()
}


kotlin {
    explicitApi()

    target {
        compilations.all {
            compilerOptions.configure {
                apiVersion.set(KotlinVersion.KOTLIN_1_8)
                languageVersion.set(KotlinVersion.KOTLIN_1_8)
            }
        }

        compilations.named("main").configure {
            compilerOptions.configure {
                apiVersion.set(KotlinVersion.KOTLIN_1_4)
            }
        }
    }
}

val shade = configurations.create("shade") {
    exclude(group = "org.jetbrains.kotlin")
}
configurations.compileOnly.configure { extendsFrom(shade) }
configurations.testRuntimeOnly.configure { extendsFrom(shade) }

gradlePlugin {
    website.set("https://github.com/TheMrMilchmann/gradle-curseforge-publish")
    vcsUrl.set("https://github.com/TheMrMilchmann/gradle-curseforge-publish.git")

    plugins {
        create("curseForgePublish") {
            id = "io.github.themrmilchmann.curseforge-publish"
            displayName = "CurseForge Gradle Publish Plugin"
            description = "A Gradle plugin for publishing to CurseForge"
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
        options.release.set(8)
    }

    withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    val relocateShadowJar = create<ConfigureShadowRelocation>("relocateShadowJar") {
        target = shadowJar.get()
    }

    jar {
        enabled = false
    }

    shadowJar {
        dependsOn(relocateShadowJar)

        archiveClassifier.set(null as String?)
        configurations = listOf(shade)
    }

    withType<Test> {
        dependsOn(shadowJar)

        useJUnitPlatform()

        doFirst {
            systemProperty("PLUGIN_CLASSPATH", shadowJar.get().outputs.files.asPath)
        }
    }

    validatePlugins {
        enableStricterValidation.set(true)
    }
}

artifacts {
    runtimeOnly(tasks.shadowJar)
    archives(tasks.shadowJar)
}

val emptyJar = tasks.create<Jar>("emptyJar") {
    destinationDirectory.set(buildDir.resolve("emptyJar"))
    archiveBaseName.set("io.github.themrmilchmann.curseforge-publish.gradle.plugin")
}

publishing {
    publications.withType<MavenPublication> {
        if (name == "curseForgePublishPluginMarkerMaven") {
            artifact(emptyJar)
            artifact(emptyJar) { classifier = "javadoc" }
            artifact(emptyJar) { classifier = "sources" }
        }

        pom {
            name.set("CurseForge Gradle Publish")
            description.set("A Gradle plugin for publishing to CurseForge")

            packaging = "jar"
        }
    }
}

dependencies {
    shade(libs.kotlinx.serialization.json)
    shade(libs.ktor.client.apache)
    shade(libs.ktor.client.serialization)

    testImplementation(platform(libs.spock.bom))
    testImplementation(libs.spock.core)
}