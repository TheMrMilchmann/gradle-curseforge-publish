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
import org.gradle.api.provider.Property
import io.github.themrmilchmann.gradle.publish.curseforge.plugins.CurseForgePublishPlugin
import javax.inject.Inject

/**
 * The configuration of how to publish the different components of a project to CurseForge.
 *
 * @since   0.6.0
 *
 * @author  Leon Linhart
 */
@CurseForgePublishPluginDsl
public abstract class CurseForgePublishingExtension @Inject internal constructor(
    /**
     * [CurseForge publications][CurseForgePublication] of the project.
     *
     * See [publications] for more information.
     *
     * @since   0.6.0
     */
    public val publications: CurseForgePublicationContainer
) {

    public companion object {

        /**
         * The name of this extension when installed by the [CurseForgePublishPlugin] ("curseforge").
         *
         * @since   0.6.0
         */
        @JvmStatic
        public val NAME: String = "curseforge"

    }

    /**
     * The API token used to authenticate with CurseForge.
     *
     * **WARNING:** Avoid using hardcoded values to set this property and, instead, make sure to properly store your
     * secrets.
     *
     * @since   0.6.0
     */
    public abstract val apiToken: Property<String>

    /**
     * Configures the [CurseForge publications][CurseForgePublication] of this project.
     *
     * @since   0.6.0
     */
    public fun publications(configure: Action<CurseForgePublicationContainer>) {
        configure.execute(publications)
    }

}