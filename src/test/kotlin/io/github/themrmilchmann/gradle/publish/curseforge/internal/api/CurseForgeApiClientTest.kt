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

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [CurseForgeApiClient].
 *
 * @author  Leon Linhart
 */
class CurseForgeApiClientTest {

    private fun mockClient(successResponse: String): CurseForgeApiClient {
        val index = AtomicInteger(0)

        return CurseForgeApiClient(
            baseUrl = "https://example.com",
            apiToken = "foobar",
            httpClient = HttpClient(MockEngine { request ->
                when (index.getAndIncrement()) {
                    0 -> respond(successResponse, headers = Headers.build {
                        append(HttpHeaders.ContentType, "application/json")
                    })
                    1 -> respond("", status = HttpStatusCode.MultipleChoices)
                    2 -> respond("", status = HttpStatusCode.Unauthorized)
                    3 -> respond("", status = HttpStatusCode.InternalServerError)
                    else -> error("Unexpected request")
                }
            }) {
                install(ContentNegotiation) {
                    json()
                }
            },
            json = Json
        )
    }

    private suspend inline fun testDefaultCalls(crossinline call: suspend () -> CurseForgeApiResponse<*>) {
        var res = call()
        assertEquals(HttpStatusCode.MultipleChoices, res.httpResponse.status)
        assertInstanceOf(CurseForgeApiResponse.ClientError::class.java, res)

        res = call()
        assertEquals(HttpStatusCode.Unauthorized, res.httpResponse.status)
        assertInstanceOf(CurseForgeApiResponse.Unauthorized::class.java, res)

        res = call()
        assertEquals(HttpStatusCode.InternalServerError, res.httpResponse.status)
        assertInstanceOf(CurseForgeApiResponse.ServerError::class.java, res)
    }

    @Test
    fun testGetGameVersions(): Unit = runBlocking {
        val client = mockClient("[{\"id\":156,\"gameVersionTypeID\":17,\"name\":\"Beta1.7.3\",\"slug\":\"beta-1-7-3\",\"apiVersion\":null},{\"id\":159,\"gameVersionTypeID\":1,\"name\":\"CB1060\",\"slug\":\"cb-1060\",\"apiVersion\":null}]")

        val gameVersions = client.getGameVersions()
        assertEquals("https://example.com/api/game/versions", gameVersions.httpResponse.request.url.toString())
        assertInstanceOf(CurseForgeApiResponse.Success::class.java, gameVersions)
        assertEquals(2, (gameVersions as CurseForgeApiResponse.Success).body().size)
        assertTrue(gameVersions.httpResponse.request.headers.contains("X-Api-Token", "foobar"))

        testDefaultCalls(client::getGameVersions)
    }

    @Test
    fun testGetGameVersionTypes(): Unit = runBlocking {
        val client = mockClient("[{\"id\":2,\"name\":\"Java\",\"slug\":\"java\"},{\"id\":4,\"name\":\"Minecraft1.8\",\"slug\":\"minecraft-1-8\"}]")

        val gameVersionTypes = client.getGameVersionTypes()
        assertEquals("https://example.com/api/game/version-types", gameVersionTypes.httpResponse.request.url.toString())
        assertInstanceOf(CurseForgeApiResponse.Success::class.java, gameVersionTypes)
        assertEquals(2, (gameVersionTypes as CurseForgeApiResponse.Success).body().size)
        assertTrue(gameVersionTypes.httpResponse.request.headers.contains("X-Api-Token", "foobar"))

        testDefaultCalls(client::getGameVersions)
    }

}