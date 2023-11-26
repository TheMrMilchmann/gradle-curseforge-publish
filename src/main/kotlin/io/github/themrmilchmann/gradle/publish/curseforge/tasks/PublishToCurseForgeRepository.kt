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
import io.github.themrmilchmann.gradle.publish.curseforge.internal.CurseForgePublicationArtifactInternal
import io.github.themrmilchmann.gradle.publish.curseforge.internal.model.api.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.model.api.CFGameVersion
import io.github.themrmilchmann.gradle.publish.curseforge.internal.utils.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.work.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@DisableCachingByDefault(because = "Not worth caching")
public open class PublishToCurseForgeRepository : AbstractPublishToCurseForge() {

    private companion object {

        val LOGGER: Logger = LoggerFactory.getLogger(PublishToCurseForgeRepository::class.java)

        private const val BASE_URL = "https://minecraft.curseforge.com"

        val json = Json {
            ignoreUnknownKeys = true
        }

    }

    @get:Input
    public val apiToken: Property<String> = project.objects.property(String::class.java)

    @TaskAction
    public fun publish(): Unit = runBlocking {
        val apiKey = apiToken.finalizeAndGetOrNull() ?: error("CurseForge API key has not been provided")

        val httpClient = HttpClient(Apache) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val publication = publicationInternal!!
        val projectId = publication.projectId.finalizeAndGet()

        val recognizedGameDependencies = httpClient.resolveGameDependencies(apiKey)
        val recognizedGameVersions = httpClient.resolveGameVersions(apiKey)

        val gameVersions = publication.gameVersions.finalizeAndGet() + publication.javaVersions.finalizeAndGet().map { GameVersion(type = "java", version = "java-${it.majorVersion}") }
        val gameVersionIDs = gameVersions.mapNotNull { gameVersion ->
            val dependency = recognizedGameDependencies.find { it.slug == gameVersion.type }
            if (dependency == null) {
                LOGGER.warn("Could not find game version for type '{}', available: {}", gameVersion.type, recognizedGameVersions)
                return@mapNotNull null
            }

            val version = recognizedGameVersions.find { it.gameVersionTypeID == dependency.id && it.slug == gameVersion.version }
            if (version == null) {
                LOGGER.warn("Could not find game version for type '{}' @ '{}', available: {}", gameVersion.type, gameVersion.version, recognizedGameVersions.filter { it.gameVersionTypeID == dependency.id })
                return@mapNotNull null
            }

            version.id
        }

        val mainArtifactID = httpClient.doUploadFile(
            publication.mainArtifact,
            projectId = projectId,
            gameVersionIds = gameVersionIDs
        )

        publication.extraArtifacts.forEach { artifact ->
            httpClient.doUploadFile(
                artifact,
                projectId = projectId,
                parentFileId = mainArtifactID
            )
        }
    }

    private suspend fun HttpClient.doUploadFile(
        artifact: CurseForgePublicationArtifactInternal,
        projectId: String,
        parentFileId: Int? = null,
        gameVersionIds: List<Int>? = null
    ): Int {
        require((parentFileId != null) xor (gameVersionIds != null)) { "Exactly one of the parameter must be set: parentFileID, gameVersionIDs" }

        val apiKey = apiToken.get()
        val metadata = generateArtifactMetadata(artifact, parentFileId = parentFileId, gameVersionIds = gameVersionIds)

        val httpResponse = submitFormWithBinaryData(
            url = "$BASE_URL/api/projects/${projectId}/upload-file",
            formData = formData {
                append(
                    "metadata",
                    Json.encodeToString(metadata),
                    headersOf(HttpHeaders.ContentType, "application/json")
                )

                append(
                    "file",
                    artifact.file.readBytes(),
                    headersOf(HttpHeaders.ContentDisposition, "filename=${artifact.file.name}")
                )
            }
        ) {
            header("X-Api-Token", apiKey)
        }

        if (httpResponse.status.isSuccess()) {
            return httpResponse.body<CFUploadResponse>().id
        } else if (httpResponse.contentType() == ContentType.Application.Json) {
            error(httpResponse.body<String>())
        } else {
            error("Publishing CurseForge publication '${publication!!.name}' failed with status code: ${httpResponse.status}")
        }
    }

    private fun generateArtifactMetadata(
        artifact: CurseForgePublicationArtifactInternal,
        parentFileId: Int? = null,
        gameVersionIds: List<Int>? = null
    ): CFUploadMetadata {
        val projectRelations = artifact.relations.mapNotNull { artifactRelation ->
            artifactRelation.type.toModelType().let {
                CFUploadMetadata.Relations.ProjectRelation(
                    slug = artifactRelation.slug,
                    type = it
                )
            }
        }

        return CFUploadMetadata(
            changelog = artifact.changelog.content.get(),
            changelogType = artifact.changelog.format.get().toJSONType(),
            displayName = artifact.displayName.get(),
            parentFileID = parentFileId,
            gameVersions = gameVersionIds,
            releaseType = artifact.releaseType.get().toJSONType(),
            relations = if (projectRelations.isNotEmpty()) CFUploadMetadata.Relations(projects = projectRelations) else null
        )
    }

    private suspend fun HttpClient.resolveGameDependencies(apiKey: String) =
        get("$BASE_URL/api/game/version-types") {
            header("X-Api-Token", apiKey)
        }.body<List<CFGameDependency>>()

    private suspend fun HttpClient.resolveGameVersions(apiKey: String) =
        get("$BASE_URL/api/game/versions") {
            header("X-Api-Token", apiKey)
        }.body<List<CFGameVersion>>()


    private fun RelationType.toModelType(): CFUploadMetadata.Relations.ProjectRelation.Type = when (this) {
        RelationType.EMBEDDED_LIBRARY -> CFUploadMetadata.Relations.ProjectRelation.Type.EMBEDDED_LIBRARY
        RelationType.INCOMPATIBLE -> CFUploadMetadata.Relations.ProjectRelation.Type.INCOMPATIBLE
        RelationType.OPTIONAL_DEPENDENCY -> CFUploadMetadata.Relations.ProjectRelation.Type.OPTIONAL_DEPENDENCY
        RelationType.REQUIRED_DEPENDENCY -> CFUploadMetadata.Relations.ProjectRelation.Type.REQUIRED_DEPENDENCY
        RelationType.TOOL -> CFUploadMetadata.Relations.ProjectRelation.Type.TOOL
    }

    private fun ChangelogFormat.toJSONType(): String = when (this) {
        ChangelogFormat.HTML -> "html"
        ChangelogFormat.MARKDOWN -> "markdown"
        ChangelogFormat.TEXT -> "text"
    }

    private fun ReleaseType.toJSONType(): String = when (this) {
        ReleaseType.ALPHA -> "alpha"
        ReleaseType.BETA -> "beta"
        ReleaseType.RELEASE -> "release"
    }

}