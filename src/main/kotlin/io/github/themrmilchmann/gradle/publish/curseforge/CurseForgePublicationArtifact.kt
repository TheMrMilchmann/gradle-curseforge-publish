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

import org.gradle.api.Action
import org.gradle.api.Buildable
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import java.io.File

/**
 * An artifact published as part of a [CurseForgePublication].
 *
 * @since   0.6.0
 *
 * @author  Leon Linhart
 */
public interface CurseForgePublicationArtifact : Buildable {

    /*
     * Note: Ideally, we'd want to extend PublicationArtifact or, at least, Buildable here. However, this does not work
     * nicely as the former has an internal protocol and the latter requires TaskDependency instances which we cannot
     * obtain using the public API.
     */

    /**
     * The name of the artifact.
     *
     * This is the name that is used to refer to this artifact during the build. This information is not published to
     * CurseForge.
     *
     * @since   0.6.0
     */
    @get:Internal
    public val name: String

    /**
     * The display name of the artifact.
     *
     * @since   0.6.0
     */
    @get:Input
    public val displayName: Property<String>

    /**
     * The release type of the artifact.
     *
     * @since   0.6.0
     */
    @get:Input
    public val releaseType: Property<ReleaseType>

    @get:InputFile
    public val file: File

    /**
     * TODO doc
     *
     * @since   0.6.0
     */
    public fun from(any: Any)

    /**
     * The changelog of the artifact.
     *
     * See [changelog] for more information.
     *
     * @since   0.6.0
     */
    @get:Nested
    public val changelog: Changelog

    /**
     * Configures the changelog of this artifact.
     *
     * @since   0.6.0
     */
    public fun changelog(configure: Action<Changelog>) {
        configure.execute(changelog)
    }

    /**
     * TODO doc
     *
     * @since   0.6.0
     */
    @get:Nested
    public val relations: ArtifactRelations

    public fun relations(configure: Action<ArtifactRelationHandler>) {
        val handler = object : ArtifactRelationHandler {
            override fun add(type: RelationType, slug: String) {
                relations.add(type, slug)
            }
        }

        configure.execute(handler)
    }

}