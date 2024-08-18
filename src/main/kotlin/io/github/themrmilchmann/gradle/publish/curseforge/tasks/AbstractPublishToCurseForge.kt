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
package io.github.themrmilchmann.gradle.publish.curseforge.tasks

import io.github.themrmilchmann.gradle.publish.curseforge.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.CurseForgePublicationArtifactInternal
import io.github.themrmilchmann.gradle.publish.curseforge.internal.publication.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.work.*
import java.util.concurrent.Callable

/**
 * Base class for tasks that publish a [CurseForgePublication].
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
public abstract class AbstractPublishToCurseForge : DefaultTask() {

    @get:Internal
    internal var publicationInternal: CurseForgePublicationInternal? = null

    @get:Internal
    internal var publication: CurseForgePublication?
        get() = publicationInternal
        set(value) { publicationInternal = value.asPublicationInternal() }

    init {
        // Allow the publication to participate in incremental build
        inputs.files(Callable {
            val publication = publicationInternal ?: return@Callable null
            publication.artifacts.map { (it as CurseForgePublicationArtifactInternal).publishableFiles }
        })
            .withPropertyName("publication.publishableFiles")
            .withPathSensitivity(PathSensitivity.NAME_ONLY)
    }

    private fun CurseForgePublication?.asPublicationInternal(): CurseForgePublicationInternal? = when (this) {
        null -> null
        is CurseForgePublicationInternal -> this
        else -> throw InvalidUserDataException("CurseForgePublication objects must implement the '${CurseForgePublicationInternal::class.qualifiedName}' interface, implementation '${this::class.qualifiedName}' does not")
    }

}