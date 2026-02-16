/*
 * Copyright (c) 2022-2026 Leon Linhart
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

import io.github.themrmilchmann.gradle.publish.curseforge.CurseForgePublicationArtifact
import org.gradle.api.artifacts.*
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.tasks.TaskDependencyContainer
import org.gradle.api.provider.*
import org.gradle.api.tasks.bundling.*
import javax.inject.Inject

internal open class CurseForgeArtifactNotationParser @Inject constructor(
    private val fileResolver: FileResolver
) {

    fun parse(any: Any): CurseForgeArtifactProvider = when (any) {
        is AbstractArchiveTask -> ArchiveTaskBasedCurseForgeArtifactProvider(any)
        is Provider<*> -> LazyCurseForgeArtifactProvider(any)
        is PublishArtifact -> PublishArtifactBasedCurseForgeArtifactProvider(any)
        is CurseForgePublicationArtifact -> CurseForgePublicationArtifactBasedCurseForgeArtifactProvider(any)
        else -> parseFileNotation(any) ?: error("Failed to parse artifact notation: $any")
    }

    private fun parseFileNotation(notation: Any): CurseForgeArtifactProvider? {
        val file = runCatching { fileResolver.asNotationParser().parseNotation(notation) }.getOrNull() ?: return null
        val taskDependencyContainer = if (notation is TaskDependencyContainer) notation else null
        val artifact = FileBasedCurseForgeArtifactProvider(file, taskDependencyContainer)

//        if (notation is TaskDependencyContainer) {
//            artifact.builtBy(
//                if (notation is Provider<*>)
//                    TaskDependencyContainer { context -> context.add(notation) }
//                else
//                    notation
//            )
//        }

        return artifact
    }

}
