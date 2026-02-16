/*
 * Copyright (c) 2022-2026 Leon Linhart
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
import io.github.themrmilchmann.gradle.publish.curseforge.internal.api.CurseForgeApiResponse
import io.github.themrmilchmann.gradle.publish.curseforge.internal.api.model.CFGameVersion
import io.github.themrmilchmann.gradle.publish.curseforge.internal.utils.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.work.*
import javax.inject.Inject

/**
 * Publishes a [CurseForgePublication] to CurseForge.
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
@DisableCachingByDefault(because = "Cannot be cached effectively")
public open class PublishToCurseForgeRepository @Inject internal constructor(
    objectFactory: ObjectFactory,
    providerFactory: ProviderFactory
) : AbstractPublishToCurseForge() {

    /**
     * The base URL of the CurseForge Upload API to target.
     *
     * @since   0.6.0
     */
    @get:Input
    public val baseUrl: Property<String> = objectFactory.property(String::class.java)
        .convention(providerFactory.gradleProperty("gradle-curseforge-publish.internal.base-url").orElse("https://minecraft.curseforge.com"))

    /**
     * The API token to use to authorize the publication.
     *
     * @since   0.6.0
     */
    @get:Input
    public val apiToken: Property<String> = objectFactory.property(String::class.java)

    @TaskAction
    public fun publish(): Unit = runBlocking {
        val baseUrl = baseUrl.finalizeAndGet()
        val apiToken = apiToken.finalizeAndGetOrNull() ?: error("CurseForge API key has not been provided")

        val publication = publicationInternal!!
        val projectId = publication.projectId.finalizeAndGet()

        CurseForgeApiClient(baseUrl = baseUrl, apiToken = apiToken).use { cfApiClient ->
            val recognizedGameDependencies = cfApiClient.getGameVersionTypes().unwrap()
            val recognizedGameVersions: List<CFGameVersion> = cfApiClient.getGameVersions().unwrap()

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
            ).unwrap().id

            logger.info("Published main artifact (artifact $mainArtifactId) of publication '${publication.name}' to CurseForge (project $projectId)")

            publication.extraArtifacts.forEach { artifact ->
                val extraArtifactId = try {
                    cfApiClient.uploadFile(
                        artifact,
                        projectId = projectId,
                        parentFileId = mainArtifactId
                    ).unwrap().id
                } catch (t: Throwable) {
                    logger.error(
                        """
                        ATTENTION!
                        
                        Publishing to CurseForge failed unexpectedly. This can lead to incomplete publications.
                        Please go to CurseForge and correct the publication manually.
                        
                        ATTENTION!
                        """.removeSurrounding("\n").trimIndent()
                    )

                    throw t
                }

                logger.info("Published '${artifact.name}' artifact (artifact $extraArtifactId) of publication '${publication.name}' to CurseForge (project $projectId)")
            }

            logger.info("Published publication '${publication.name}' to CurseForge (project $projectId)")
        }
    }

    private suspend fun <T>  CurseForgeApiResponse<T>.unwrap(): T = when (this) {
        is CurseForgeApiResponse.Success -> {
            logger.debug("Successfully called CurseForge Upload API: {}", httpResponse.call.request.url)
            body()
        }
        is CurseForgeApiResponse.Unauthorized -> {
            logger.error("CurseForge authentication failed. Make sure you specified a valid CurseForge API token")
            error("CurseForge authentication failed")
        }
        is CurseForgeApiResponse.ClientError -> {
            logger.error(
                """
                CurseForge Upload API call failed.
                Please update to the latest version of https://github.com/TheMrMilchmann/gradle-curseforge-publish
                or report this issue to the plugin maintainers if it persists.
                
                Failed call: {}
                Response body:
                {}
                """.removeSurrounding("\n").trimIndent(), httpResponse.call.request.url, httpResponse.bodyAsText())
            error("CurseForge Upload API call failed (client)")
        }
        is CurseForgeApiResponse.ServerError -> {
            logger.error(
                """
                CurseForge Upload API returned unexpected response.
                Please try again later or report this issue to the plugin maintainers if it persists.
                """.removeSurrounding("\n").trimIndent())
            error("CurseForge Upload API call failed (server)")
        }
    }

}
