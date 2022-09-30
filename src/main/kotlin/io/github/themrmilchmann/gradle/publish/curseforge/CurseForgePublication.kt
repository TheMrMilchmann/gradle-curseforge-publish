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
package io.github.themrmilchmann.gradle.publish.curseforge

import org.gradle.api.*
import org.gradle.api.provider.*
import org.gradle.api.publish.*

public interface CurseForgePublication : Publication {

    public val projectID: Property<Int>

    public fun artifact(artifact: Any)

    public fun artifact(action: Action<CurseForgeArtifact>)

    public fun artifact(artifact: Any, action: Action<CurseForgeArtifact>)

    public fun includeGameVersions(filter: (type: String, version: String) -> Boolean)

    /**
     * The supported Java versions.
     *
     * The set may contain predefined entries. Do not overwrite this value
     * unless you specifically want to overwrite all preset/inferred entries.
     *
     * The set's entries are converted to strings using the [toString][Any.toString]
     * function. Entries of type [Provider] are unwrapped first. The returned
     * string should match the major version component of the supported Java
     * version.
     */
    public var javaVersions: Set<Any>

    /** Adds the given [version] to the list of [supported Java versions][javaVersions]. */
    public fun javaVersion(version: String)

    /** Adds the given [version] to the list of [supported Java versions][javaVersions]. */
    public fun javaVersion(version: Provider<String>)

    /** Adds the given [versions] to the list of [supported Java versions][javaVersions]. */
    public fun javaVersions(vararg versions: String)

}