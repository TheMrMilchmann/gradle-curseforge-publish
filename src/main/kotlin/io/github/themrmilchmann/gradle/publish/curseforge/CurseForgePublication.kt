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

import org.gradle.api.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

/**
 * A `CurseForgePublication` is the representation/configuration of how Gradle should publish something to CurseForge.
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
@CurseForgePublishPluginDsl
public interface CurseForgePublication {

    /**
     * The name that of this publication.
     *
     * This is the name that is used to refer to this publication during the build. This information is not published to
     * CurseForge.
     *
     * @since   0.6.0
     */
    @get:Internal
    public val name: String

    /**
     * The ID of the CurseForge project that this publication is published to.
     *
     * @since   0.6.0
     */
    @get:Input
    public val projectId: Property<String>

    /**
     * The supported [game versions][GameVersion].
     *
     * @since   0.5.0
     */
    @get:Input
    public val gameVersions: SetProperty<GameVersion>

    /**
     * Adds a version to this publication's supported [game versions][gameVersions].
     *
     * @since   0.6.0
     */
    public fun gameVersion(type: String, version: String) {
        gameVersions.add(GameVersion(type, version))
    }

    /**
     * The supported Java versions.
     *
     * @since   0.5.0
     */
    @get:Input
    public val javaVersions: SetProperty<JavaVersion>

    /**
     * Adds a version to this publication's supported [java versions][javaVersions].
     *
     * @since   0.6.0
     */
    public fun javaVersion(version: JavaVersion) {
        javaVersions.add(version)
    }

    /**
     * The artifacts of the publication.
     *
     * See [artifacts] for more information.
     *
     * @since   0.6.0
     */
    @get:Nested
    public val artifacts: CurseForgePublicationArtifactContainer

    /**
     * Configures the artifacts of this publication.
     *
     * Each publication must have a primary artifact when publishing to CurseForge. The primary artifact of a
     * publication is named "main". When a publication is implicitly created by the plugin, the main artifact is
     * automatically configured. Otherwise, an artifact can be added as follows:
     *
     * ```kotlin
     * artifacts {
     *     register("main") {
     *         from(...)
     *     }
     * }
     * ```
     *
     * @param configure the action or closure to configure the artifacts with
     *
     * @since   0.6.0
     */
    public fun artifacts(configure: Action<CurseForgePublicationArtifactContainer>) {
        configure.execute(artifacts)
    }

}