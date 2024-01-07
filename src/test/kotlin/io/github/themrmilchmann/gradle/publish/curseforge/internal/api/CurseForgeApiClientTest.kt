/*
 * Copyright (c) 2021-2023 Leon Linhart,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * 4. Redistributions of the software or derivative works, in any form, for
 *    commercial purposes, are strictly prohibited without the express written
 *    consent of the copyright holder. For the purposes of this license,
 *    "commercial purposes" includes, but is not limited to, redistributions
 *    through applications or services that generate revenue through
 *    advertising. This clause does neither not apply to "community
 *    contributions", as defined by the act of submitting changes or additions
 *    to the software, provided that the contributors are not copyright
 *    holders of the software, nor does it apply to derivatives of the software.
 *
 * 5. Any modifications or derivatives of the software, whether in source or
 *    binary form, must be made publicly available under the same license terms
 *    as this original license. This includes providing access to the modified
 *    source code.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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