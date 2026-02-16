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
package io.github.themrmilchmann.gradle.publish.curseforge.internal.api

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.util.reflect.*

internal sealed class CurseForgeApiResponse<T>(val httpResponse: HttpResponse) {

    class Success<T : Any>(
        private val valueTypeInfo: TypeInfo,
        httpResponse: HttpResponse
    ) : CurseForgeApiResponse<T>(httpResponse) {
        suspend fun body(): T = httpResponse.body(valueTypeInfo) as T
    }

    class ClientError(httpResponse: HttpResponse) : CurseForgeApiResponse<Nothing>(httpResponse)
    class Unauthorized(httpResponse: HttpResponse) : CurseForgeApiResponse<Nothing>(httpResponse)
    class ServerError(httpResponse: HttpResponse) : CurseForgeApiResponse<Nothing>(httpResponse)

}
