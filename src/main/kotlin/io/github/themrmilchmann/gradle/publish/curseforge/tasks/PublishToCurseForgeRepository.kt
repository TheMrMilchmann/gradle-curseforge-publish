/*
 * Copyright (c) 2021-2022 Leon Linhart
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
import io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts.repositories.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.model.api.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.utils.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.work.*
import java.io.*

@DisableCachingByDefault(because = "Not worth caching")
public open class PublishToCurseForgeRepository : AbstractPublishToCurseForge() {

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
        }
    }

    private var _repository: CurseForgeArtifactRepository? = null

    private val apiKey: Property<String> = project.objects.property(String::class.java)

    @get:Internal
    public var repository: CurseForgeArtifactRepository?
        get() = _repository
        set(value) {
            _repository = value!!
            apiKey.set(value.apiKey)
        }

    private val url: String
        get() = (repository as DefaultCurseForgeArtifactRepository).url

    @TaskAction
    public fun publish(): Unit = runBlocking {
        val apiKey = apiKey.finalizeAndGetOrNull() ?: error("CurseForge API key has not been provided for repository: ${_repository!!.name}")

        val httpClient = HttpClient(Apache) {
            install(JsonFeature) {
                serializer = KotlinxSerializer(json)
            }
        }

        val publication = publicationInternal!!

        val projectID = publication.projectID.finalizeAndGet()

        val gameDependencies = httpClient.resolveGameDependencies(apiKey)
        val gameVersions = httpClient.resolveGameVersions(apiKey)

        val gameVersionIDs = gameDependencies.flatMap { dep ->
            gameVersions.filter { version -> version.gameVersionTypeID == dep.id }
                .mapNotNull { version ->
                    if (publication.isGameVersionIncluded(dep, version))
                        version.id
                    else
                        null
                }
        }

        val mainArtifactID = httpClient.doUploadFile(
            publication.mainArtifact,
            projectID = projectID,
            gameVersionIDs = gameVersionIDs
        )

        publication.extraArtifacts.forEach { artifact ->
            httpClient.doUploadFile(
                artifact,
                projectID = projectID,
                parentFileID = mainArtifactID
            )
        }
    }

    private suspend fun HttpClient.doUploadFile(
        artifact: AbstractCurseForgeArtifact,
        projectID: Int,
        parentFileID: Int? = null,
        gameVersionIDs: List<Int>? = null
    ): Int {
        require((parentFileID != null) xor (gameVersionIDs != null)) { "Exactly one of the parameter must be set: parentFileID, gameVersionIDs" }

        val apiKey = apiKey.get()

        val metadata = json.decodeFromString<UploadMetadata>(File("${artifact.file.absolutePath}.metadata.json").readText())
            .copy(parentFileID = parentFileID, gameVersions = gameVersionIDs)

        val httpResponse = submitFormWithBinaryData<HttpResponse>(
            url = "${this@PublishToCurseForgeRepository.url}/api/projects/${projectID}/upload-file",
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
            return httpResponse.receive<UploadResponse>().id
        } else if (httpResponse.contentType() == ContentType.Application.Json) {
            error(httpResponse.receive<String>())
        } else {
            error("Publishing CurseForge publication '${publication!!.name}' to ${repository!!.name} failed with status code: ${httpResponse.status}")
        }
    }

    private suspend fun HttpClient.resolveGameDependencies(apiKey: String) =
        get<List<GameDependency>>("$url/api/game/version-types") {
            header("X-Api-Token", apiKey)
        }

    private suspend fun HttpClient.resolveGameVersions(apiKey: String) =
        get<List<GameVersion>>("$url/api/game/versions") {
            header("X-Api-Token", apiKey)
        }

}