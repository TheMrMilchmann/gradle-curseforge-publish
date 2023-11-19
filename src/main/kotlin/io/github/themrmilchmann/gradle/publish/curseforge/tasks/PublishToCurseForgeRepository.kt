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
import io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts.repositories.*
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
import java.io.*

@DisableCachingByDefault(because = "Not worth caching")
public open class PublishToCurseForgeRepository : AbstractPublishToCurseForge() {

    private companion object {

        val LOGGER: Logger = LoggerFactory.getLogger(PublishToCurseForgeRepository::class.java)

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
            install(ContentNegotiation) {
                json(json)
            }
        }

        val publication = publicationInternal!!

        val projectID = publication.projectID.finalizeAndGet()

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

        val metadata = json.decodeFromString<CFUploadMetadata>(File("${artifact.file.absolutePath}.metadata.json").readText())
            .copy(parentFileID = parentFileID, gameVersions = gameVersionIDs)

        val httpResponse = submitFormWithBinaryData(
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
            return httpResponse.body<CFUploadResponse>().id
        } else if (httpResponse.contentType() == ContentType.Application.Json) {
            error(httpResponse.body<String>())
        } else {
            error("Publishing CurseForge publication '${publication!!.name}' to ${repository!!.name} failed with status code: ${httpResponse.status}")
        }
    }

    private suspend fun HttpClient.resolveGameDependencies(apiKey: String) =
        get("$url/api/game/version-types") {
            header("X-Api-Token", apiKey)
        }.body<List<CFGameDependency>>()

    private suspend fun HttpClient.resolveGameVersions(apiKey: String) =
        get("$url/api/game/versions") {
            header("X-Api-Token", apiKey)
        }.body<List<CFGameVersion>>()

}