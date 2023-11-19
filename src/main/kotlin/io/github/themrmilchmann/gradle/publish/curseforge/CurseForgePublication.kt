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
package io.github.themrmilchmann.gradle.publish.curseforge

import org.gradle.api.*
import org.gradle.api.provider.*
import org.gradle.api.publish.*

/**
 * TODO doc
 *
 * @since   0.1.0
 */
public interface CurseForgePublication : Publication {

    /**
     * TODO doc
     *
     * @since   0.1.0
     */
    public val projectID: Property<Int>

    /**
     * TODO doc
     *
     * @since   0.1.0
     */
    public fun artifact(artifact: Any)

    /**
     * TODO doc
     *
     * @since   0.1.0
     */
    public fun artifact(action: Action<CurseForgeArtifact>)

    /**
     * TODO doc
     *
     * @since   0.1.0
     */
    public fun artifact(artifact: Any, action: Action<CurseForgeArtifact>)

    /**
     * The supported [game versions][GameVersion].
     *
     * @since   0.5.0
     */
    public val gameVersions: SetProperty<GameVersion>

    /**
     * The supported Java versions.
     *
     * @since   0.5.0
     */
    public val javaVersions: SetProperty<JavaVersion>

}