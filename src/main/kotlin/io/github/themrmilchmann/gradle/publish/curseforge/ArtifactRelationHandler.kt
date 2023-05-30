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

public interface ArtifactRelationHandler {

    public fun add(type: ArtifactRelation.Type, slug: String)

    public fun embeddedLibrary(slug: String): Unit =
        add(ArtifactRelation.Type.EmbeddedLibrary, slug)

    public fun incompatible(slug: String): Unit =
        add(ArtifactRelation.Type.Incompatible, slug)

    public fun optionalDependency(slug: String): Unit =
        add(ArtifactRelation.Type.OptionalDependency, slug)

    public fun requiredDependency(slug: String): Unit =
        add(ArtifactRelation.Type.RequiredDependency, slug)

    public fun tool(slug: String): Unit =
        add(ArtifactRelation.Type.Tool, slug)

}