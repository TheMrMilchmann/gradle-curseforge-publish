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
package io.github.themrmilchmann.gradle.publish.curseforge.internal.api

import io.github.themrmilchmann.gradle.publish.curseforge.ChangelogFormat
import io.github.themrmilchmann.gradle.publish.curseforge.RelationType
import io.github.themrmilchmann.gradle.publish.curseforge.ReleaseType
import io.github.themrmilchmann.gradle.publish.curseforge.internal.CurseForgePublicationArtifactInternal
import io.github.themrmilchmann.gradle.publish.curseforge.internal.api.model.CFGameDependency
import io.github.themrmilchmann.gradle.publish.curseforge.internal.api.model.CFGameVersion
import io.github.themrmilchmann.gradle.publish.curseforge.internal.api.model.CFUploadMetadata
import io.github.themrmilchmann.gradle.publish.curseforge.internal.api.model.CFUploadResponse
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.cio.*
import io.ktor.util.reflect.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal fun CurseForgeApiClient(
    baseUrl: String,
    apiToken: String,
    json: Json = Json {
        ignoreUnknownKeys = true
    }
): CurseForgeApiClient = CurseForgeApiClient(
    baseUrl = baseUrl,
    apiToken = apiToken,
    httpClient = HttpClient(Apache) {
        engine {
            followRedirects = true
            socketTimeout = 10_000
            connectTimeout = 10_000
            connectionRequestTimeout = 20_000
        }

        install(ContentNegotiation) {
            json(json)
        }
    },
    json = json
)

internal class CurseForgeApiClient(
    private val baseUrl: String,
    private val apiToken: String,
    private val httpClient: HttpClient,
    private val json: Json
) : AutoCloseable by httpClient {

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> wrap(httpResponse: HttpResponse): CurseForgeApiResponse<T> = when (httpResponse.status.value) {
        in (200 until 300), in (301 until 400) -> CurseForgeApiResponse.Success(typeInfo<T>(), httpResponse)
        HttpStatusCode.Unauthorized.value -> CurseForgeApiResponse.Unauthorized(httpResponse) as CurseForgeApiResponse<T>
        in (500 until 600) -> CurseForgeApiResponse.ServerError(httpResponse) as CurseForgeApiResponse<T>
        else -> CurseForgeApiResponse.ClientError(httpResponse) as CurseForgeApiResponse<T>
    }

    suspend fun getGameVersions(): CurseForgeApiResponse<List<CFGameVersion>> {
        val url = URLBuilder(baseUrl).run {
            path("/api/game/versions")
            build()
        }

        val httpResponse = httpClient.get(url) {
            header("X-Api-Token", apiToken)
        }

        return wrap(httpResponse)
    }

    suspend fun getGameVersionTypes(): CurseForgeApiResponse<List<CFGameDependency>> {
        val url = URLBuilder(baseUrl).run {
            path("/api/game/version-types")
            build()
        }

        val httpResponse = httpClient.get(url) {
            header("X-Api-Token", apiToken)
        }

        return wrap(httpResponse)
    }

    suspend fun uploadFile(
        artifact: CurseForgePublicationArtifactInternal,
        projectId: String,
        parentFileId: Int? = null,
        gameVersionIds: List<Int>? = null
    ): CurseForgeApiResponse<CFUploadResponse> {
        require((parentFileId != null) xor (gameVersionIds != null)) { "Exactly one of the parameter must be set: parentFileID, gameVersionIDs" }

        val url = URLBuilder(baseUrl).run {
            path("/api/projects/${projectId}/upload-file")
            build()
        }

        val metadata = generateArtifactMetadata(artifact, parentFileId = parentFileId, gameVersionIds = gameVersionIds)

        val httpResponse = httpClient.submitFormWithBinaryData(formData = formData {
            append(
                key = "metadata",
                value = json.encodeToString(metadata),
                headersOf(HttpHeaders.ContentType, "application/json")
            )

            append(
                key = "file",
                value = ChannelProvider(artifact.file.length(), artifact.file::readChannel),
                headersOf(HttpHeaders.ContentDisposition, "filename=${artifact.file.name}")
            )
        }) {
            url(url)
            header("X-Api-Token", apiToken)
        }

        return wrap(httpResponse)
    }

    private fun generateArtifactMetadata(
        artifact: CurseForgePublicationArtifactInternal,
        parentFileId: Int? = null,
        gameVersionIds: List<Int>? = null
    ): CFUploadMetadata {
        val projectRelations = artifact.relations.map { artifactRelation ->
            CFUploadMetadata.Relations.ProjectRelation(
                slug = artifactRelation.slug,
                type = artifactRelation.type.toModelType()
            )
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