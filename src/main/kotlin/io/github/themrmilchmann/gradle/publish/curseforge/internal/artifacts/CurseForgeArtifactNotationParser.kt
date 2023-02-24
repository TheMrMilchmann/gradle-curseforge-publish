/*
 * Copyright (c) 2022 Leon Linhart
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
package io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts

import org.gradle.api.artifacts.*
import org.gradle.api.internal.artifacts.dsl.*
import org.gradle.api.internal.file.*
import org.gradle.api.internal.tasks.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.bundling.*

internal class CurseForgeArtifactNotationParser(
    private val fileResolver: FileResolver,
    private val taskDependencyFactory: TaskDependencyFactory
) {

    fun parse(any: Any): AbstractCurseForgeArtifact = when (any) {
        is AbstractArchiveTask -> parseArchiveTaskNotation(any)
        is Provider<*> -> parseProviderNotation(any)
        is PublishArtifact -> parsePublishArtifactNotation(any)
        else -> parseFileNotation(any) ?: error("Failed to parse artifact notation: $any")
    }

    private fun parseArchiveTaskNotation(archiveTask: AbstractArchiveTask): AbstractCurseForgeArtifact =
        ArchiveTaskBasedCurseForgeArtifact(archiveTask, taskDependencyFactory)

    private fun parseFileNotation(notation: Any): AbstractCurseForgeArtifact? {
        val file = runCatching { fileResolver.asNotationParser().parseNotation(notation) }.getOrNull() ?: return null
        val artifact = FileBasedCurseForgeArtifact(file, taskDependencyFactory)

        if (notation is TaskDependencyContainer) {
            artifact.builtBy(
                if (notation is Provider<*>)
                    TaskDependencyContainer { context -> context.add(notation) }
                else
                    notation
            )
        }

        return artifact
    }

    private fun parseProviderNotation(provider: Provider<*>): AbstractCurseForgeArtifact =
        PublishArtifactBasedCurseForgeArtifact(LazyPublishArtifact(provider, fileResolver, taskDependencyFactory), taskDependencyFactory)

    private fun parsePublishArtifactNotation(publishArtifact: PublishArtifact): AbstractCurseForgeArtifact =
        PublishArtifactBasedCurseForgeArtifact(publishArtifact, taskDependencyFactory)

}