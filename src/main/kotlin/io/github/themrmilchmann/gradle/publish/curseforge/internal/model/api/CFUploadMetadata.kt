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
package io.github.themrmilchmann.gradle.publish.curseforge.internal.model.api

import kotlinx.serialization.*

@Serializable
internal data class CFUploadMetadata(
    val changelog: String,
    val changelogType: String,
    val displayName: String? = null,
    val parentFileID: Int? = null,
    val gameVersions: List<Int>? = null,
    val releaseType: String,
    val relations: Relations? = null
) {

    @Serializable
    internal data class Relations(
        val projects: List<ProjectRelation>
    ) {

        @Serializable
        internal data class ProjectRelation(
            val slug: String,
            val type: Type
        ) {

            @Serializable
            internal enum class Type {
                @SerialName("embeddedLibrary")
                EMBEDDED_LIBRARY,
                @SerialName("incompatible")
                INCOMPATIBLE,
                @SerialName("optionalDependency")
                OPTIONAL_DEPENDENCY,
                @SerialName("requiredDependency")
                REQUIRED_DEPENDENCY,
                @SerialName("tool")
                TOOL
            }

        }

    }

}