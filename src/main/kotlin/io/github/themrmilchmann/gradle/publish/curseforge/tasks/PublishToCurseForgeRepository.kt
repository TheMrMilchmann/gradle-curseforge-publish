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
package io.github.themrmilchmann.gradle.publish.curseforge.tasks

import io.github.themrmilchmann.gradle.publish.curseforge.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.api.CurseForgeApiClient
import io.github.themrmilchmann.gradle.publish.curseforge.internal.utils.*
import kotlinx.coroutines.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.work.*
import javax.inject.Inject

@DisableCachingByDefault(because = "Not worth caching")
public open class PublishToCurseForgeRepository @Inject internal constructor(
    objectFactory: ObjectFactory,
    providerFactory: ProviderFactory
) : AbstractPublishToCurseForge() {

    @get:Input
    public val baseUrl: Property<String> = objectFactory.property(String::class.java)
        .convention(providerFactory.gradleProperty("gradle-curseforge-publish.internal.base-url").orElse("https://minecraft.curseforge.com"))

    @get:Input
    public val apiToken: Property<String> = objectFactory.property(String::class.java)

    @TaskAction
    public fun publish(): Unit = runBlocking {
        val baseUrl = baseUrl.finalizeAndGet()
        val apiToken = apiToken.finalizeAndGetOrNull() ?: error("CurseForge API key has not been provided")

        val cfApiClient = CurseForgeApiClient(baseUrl = baseUrl, apiToken = apiToken)

        val publication = publicationInternal!!
        val projectId = publication.projectId.finalizeAndGet()

        val recognizedGameDependencies = cfApiClient.getGameVersionTypes()
        val recognizedGameVersions = cfApiClient.getGameVersions()

        val gameVersions = publication.gameVersions.finalizeAndGet() + publication.javaVersions.finalizeAndGet().map { GameVersion(type = "java", version = "java-${it.majorVersion}") }
        val gameVersionIDs = gameVersions.mapNotNull { gameVersion ->
            val dependency = recognizedGameDependencies.find { it.slug == gameVersion.type }
            if (dependency == null) {
                logger.warn("Could not find game version for type '{}', available: {}", gameVersion.type, recognizedGameVersions)
                return@mapNotNull null
            }

            val version = recognizedGameVersions.find { it.gameVersionTypeID == dependency.id && it.slug == gameVersion.version }
            if (version == null) {
                logger.warn("Could not find game version for type '{}' @ '{}', available: {}", gameVersion.type, gameVersion.version, recognizedGameVersions.filter { it.gameVersionTypeID == dependency.id })
                return@mapNotNull null
            }

            version.id
        }

        val mainArtifactId = cfApiClient.uploadFile(
            publication.mainArtifact,
            projectId = projectId,
            gameVersionIds = gameVersionIDs
        ).id

        logger.info("Published main artifact (artifact $mainArtifactId) of publication '${publication.name}' to CurseForge (project $projectId)")

        publication.extraArtifacts.forEach { artifact ->
            val extraArtifactId = cfApiClient.uploadFile(
                artifact,
                projectId = projectId,
                parentFileId = mainArtifactId
            ).id

            logger.info("Published '${artifact.name}' artifact (artifact $extraArtifactId) of publication '${publication.name}' to CurseForge (project $projectId)")
        }

        logger.info("Published publication '${publication.name}' to CurseForge (project $projectId)")
    }

}