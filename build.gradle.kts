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
import org.gradle.api.publish.maven.internal.artifact.ArchiveTaskBasedMavenArtifact
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    groovy
    `java-test-fixtures`
    `kotlin-dsl`
    `maven-publish`
    signing
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.gradle.shadow)
    alias(libs.plugins.plugin.publish)
}

group = "io.github.themrmilchmann.gradle.publish.curseforge"
val nextVersion = "0.1.0"
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

gradlePlugin {
    plugins {
        create("curseForgePublish") {
            id = "io.github.themrmilchmann.curseforge-publish"
            displayName = "curseforge-publish"
            description = "Publish artifact to CurseForge"

            implementationClass = "io.github.themrmilchmann.gradle.publish.curseforge.plugins.CurseForgePublishPlugin"
        }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }

    withType<Test> {
        useJUnitPlatform()
    }

    jar {
        enabled = false

        archiveClassifier.set("invalid_removeme")
    }

    val relocateShadowJar = create<ConfigureShadowRelocation>("relocateShadowJar") {
        target = shadowJar.get()
        prefix = "io.github.themrmilchmann.gradle.publish.curseforge.internal.deps"
    }

    shadowJar {
        dependsOn(relocateShadowJar)

        archiveClassifier.set(null as String?)
    }

    publishPlugins {
        dependsOn(shadowJar)
    }
}

configurations.archives.get().apply {
    artifacts.remove(artifacts.find { it.classifier == "invalid_removeme" })
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
    // needed to prevent inclusion of gradle-api into shadow JAR
    configurations.api.get().dependencies.remove(gradleApi())

    shadow(gradleApi())
    shadow(kotlin("stdlib-jdk8"))
    shadow(localGroovy())

    implementation(libs.kotlinx.serialization.json) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
    implementation(libs.ktor.client.apache) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
    implementation(libs.ktor.client.serialization) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }

    testFixturesApi(gradleApi())

    testFixturesApi(platform(libs.spock.bom))
    testFixturesApi(libs.spock.core)
}