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
import com.github.themrmilchmann.build.*
import com.github.themrmilchmann.build.BuildType
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    groovy
    `kotlin-dsl`
    `maven-publish`
    signing
    alias(libs.plugins.gradle.shadow)
    alias(libs.plugins.gradle.toolchain.switches)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.plugin.publish)
}

group = "io.github.themrmilchmann.gradle.publish.curseforge"
val nextVersion = "0.4.0"
version = when (deployment.type) {
    BuildType.SNAPSHOT -> "$nextVersion-SNAPSHOT"
    else -> nextVersion
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
}

val shade = configurations.create("shade") {
    exclude(group = "org.jetbrains.kotlin")
}
configurations.compileOnly.configure {
    extendsFrom(shade)
}

gradlePlugin {
    plugins {
        create("curseForgePublish") {
            id = "io.github.themrmilchmann.curseforge-publish"
            displayName = "CurseForge Gradle Publish Plugin"
            description = "A Gradle plugin for publishing to CurseForge"

            implementationClass = "io.github.themrmilchmann.gradle.publish.curseforge.plugins.CurseForgePublishPlugin"
        }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }

    val relocateShadowJar = create<ConfigureShadowRelocation>("relocateShadowJar") {
        target = shadowJar.get()
    }

    shadowJar {
        dependsOn(relocateShadowJar)

        archiveClassifier.set(null as String?)
        configurations = listOf(shade)
    }

    withType<Test> {
        useJUnitPlatform()
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
    repositories {
        maven {
            url = uri(deployment.repo)

            credentials {
                username = deployment.user
                password = deployment.password
            }
        }
    }
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
            url.set("https://github.com/TheMrMilchmann/gradle-curseforge-publish")

            licenses {
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/TheMrMilchmann/gradle-curseforge-publish/blob/master/LICENSE")
                        distribution.set("repo")
                    }
                }
            }

            developers {
                developer {
                    id.set("TheMrMilchmann")
                    name.set("Leon Linhart")
                    email.set("themrmilchmann@gmail.com")
                    url.set("https://github.com/TheMrMilchmann")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/TheMrMilchmann/gradle-curseforge-publish.git")
                developerConnection.set("scm:git:git://github.com/TheMrMilchmann/gradle-curseforge-publish.git")
                url.set("https://github.com/TheMrMilchmann/gradle-curseforge-publish.git")
            }
        }
    }
}

pluginBundle {
    website = "https://github.com/TheMrMilchmann/gradle-curseforge-publish"
    vcsUrl = "https://github.com/TheMrMilchmann/gradle-curseforge-publish.git"

    tags = listOf("publishing")
}

signing {
    isRequired = (deployment.type === BuildType.RELEASE)
    sign(publishing.publications)
}

repositories {
    mavenCentral()
}

dependencies {
    shade(libs.kotlinx.serialization.json)
    shade(libs.ktor.client.apache)
    shade(libs.ktor.client.serialization)

    testImplementation(platform(libs.spock.bom))
    testImplementation(libs.spock.core)
}