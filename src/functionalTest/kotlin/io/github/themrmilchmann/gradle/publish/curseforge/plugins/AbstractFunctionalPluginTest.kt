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
package io.github.themrmilchmann.gradle.publish.curseforge.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.gradle.api.JavaVersion
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

abstract class AbstractFunctionalPluginTest {

    protected companion object {

        const val PROJECT_ID = "12345"

        private lateinit var engine: ApplicationEngine

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            engine = embeddedServer(Netty, port = 8080) {
                routing {
                    get("/api/game/version-types") {
                        call.respondText(contentType = ContentType.Application.Json) {
                            """
                            [
                                {
                                    "id": 2,
                                    "name": "Java",
                                    "slug": "java"
                                },
                                {
                                    "id": 73407,
                                    "name": "Minecraft 1.19",
                                    "slug": "minecraft-1-19"
                                },
                                {
                                    "id": 75125,
                                    "name": "Minecraft 1.20",
                                    "slug": "minecraft-1-20"
                                },
                                {
                                    "id": 68441,
                                    "name": "Modloader",
                                    "slug": "modloader"
                                },
                                {
                                    "id": 75208,
                                    "name": "Environment",
                                    "slug": "environment"
                                }
                            ]
                            """.removeSuffix("\n").trimIndent()
                        }
                    }
                    get("/api/game/versions") {
                        call.respondText(contentType = ContentType.Application.Json) {
                            """
                            [
                                {
                                    "id": 8326,
                                    "gameVersionTypeID": 2,
                                    "name": "Java 17",
                                    "slug": "java-17",
                                    "apiVersion": ""
                                },
                                {
                                    "id": 10236,
                                    "gameVersionTypeID": 75125,
                                    "name": "1.20.2",
                                    "slug": "1-20-2",
                                    "apiVersion": null
                                },
                                {
                                    "id": 7498,
                                    "gameVersionTypeID": 68441,
                                    "name": "Forge",
                                    "slug": "forge",
                                    "apiVersion": ""
                                },
                                {
                                    "id": 7499,
                                    "gameVersionTypeID": 68441,
                                    "name": "Fabric",
                                    "slug": "fabric",
                                    "apiVersion": ""
                                }
                            ]
                            """.removeSuffix("\n").trimIndent()
                        }
                    }
                    post("/api/projects/$PROJECT_ID/upload-file") {
                        call.respondText(contentType = ContentType.Application.Json) {
                            """
                            {
                                "id": 67890
                            }
                            """.removeSuffix("\n").trimIndent()
                        }
                    }
                }
            }

            engine.start()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            engine.stop()
        }

        @JvmStatic
        fun provideGradleVersions(): List<GradleVersion> = buildList {
            // See https://docs.gradle.org/current/userguide/compatibility.html
            val javaVersion = JavaVersion.current()

            add("8.10.1")
            add("8.9")
            add("8.8")
            add("8.7")
            add("8.6")
            add("8.5")
            if (javaVersion.majorVersion.toInt() >= 21) return@buildList

            add("8.4")
            add("8.3")

            if (javaVersion >= JavaVersion.VERSION_20) return@buildList

            add("8.2.1")
            add("8.1.1")
            add("8.0.2")
        }.map(::GradleVersion)

    }

}