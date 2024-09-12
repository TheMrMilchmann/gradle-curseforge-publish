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

import io.github.themrmilchmann.gradle.publish.curseforge.internal.changelog.ChangelogSourceParser
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import javax.inject.Inject

/**
 * A changelog.
 *
 * @since   0.6.0
 *
 * @author  Leon Linhart
 */
@CurseForgePublishPluginDsl
public open class Changelog @Inject internal constructor(
    objectFactory: ObjectFactory,
    private val changelogParser: ChangelogSourceParser
) {

    /**
     * The format of the changelog's [content].
     *
     * Defaults to [ChangelogFormat.TEXT].
     *
     * @since   0.6.0
     */
    @get:Input
    public val format: Property<ChangelogFormat> = objectFactory.property(ChangelogFormat::class.java)
        .convention(ChangelogFormat.TEXT)

    /**
     * The content of the changelog.
     *
     * Defaults to the empty string `""`.
     *
     * @since   0.6.0
     */
    @get:Input
    public val content: Property<String> = objectFactory.property(String::class.java)
        .convention("")

    /**
     * Sets the changelog's content.
     *
     * This method converts the supplied source to a string based on its type:
     *
     * - A [CharSequence].
     * - A [Provider][org.gradle.api.provider.Provider] of any supported type. THe provider's value is resolved
     *   recursively.
     * - A file notation as specified by [org.gradle.api.Project.file].
     *
     * The changelog's content is set using the `Property.set(provider: Provider<? extends T>)` function regardless of
     * the source provided.
     *
     * @since   0.7.0
     */
    public fun from(source: Any) {
        content.set(changelogParser.parse(source))
    }

}