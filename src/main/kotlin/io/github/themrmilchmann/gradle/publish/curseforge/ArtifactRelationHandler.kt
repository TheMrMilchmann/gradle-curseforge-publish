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
package io.github.themrmilchmann.gradle.publish.curseforge

/**
 * An `ArtifactRelationHandler` is used to declare artifact relations.
 *
 * @since   0.5.0
 *
 * @author  Leon Linhart
 */
@CurseForgePublishPluginDsl
public interface ArtifactRelationHandler {

    /**
     * Adds a relation to the given artifact.
     *
     * @param type  the type of the relation
     * @param slug  the slug of the related artifact
     *
     * @since   0.5.0
     */
    public fun add(type: RelationType, slug: String)

    /**
     * Adds an [embedded library][RelationType.EMBEDDED_LIBRARY] relation.
     *
     * @param slug  the slug of the related artifact
     *
     * @since   0.5.0
     */
    public fun embeddedLibrary(slug: String): Unit =
        add(RelationType.EMBEDDED_LIBRARY, slug)

    /**
     * Adds an [incompatible][RelationType.INCOMPATIBLE] relation.
     *
     * @param slug  the slug of the related artifact
     *
     * @since   0.5.0
     */
    public fun incompatible(slug: String): Unit =
        add(RelationType.INCOMPATIBLE, slug)

    /**
     * Adds an [optional dependency][RelationType.OPTIONAL_DEPENDENCY] relation.
     *
     * @param slug  the slug of the related artifact
     *
     * @since   0.5.0
     */
    public fun optionalDependency(slug: String): Unit =
        add(RelationType.OPTIONAL_DEPENDENCY, slug)

    /**
     * Adds a [required dependency][RelationType.REQUIRED_DEPENDENCY] relation.
     *
     * @param slug  the slug of the related artifact
     *
     * @since   0.5.0
     */
    public fun requiredDependency(slug: String): Unit =
        add(RelationType.REQUIRED_DEPENDENCY, slug)

    /**
     * Adds a [tool][RelationType.TOOL] relation.
     *
     * @param slug  the slug of the related artifact
     *
     * @since   0.5.0
     */
    public fun tool(slug: String): Unit =
        add(RelationType.TOOL, slug)

}