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
package io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts

import org.gradle.api.artifacts.*
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.tasks.TaskDependencyContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.*
import org.gradle.api.tasks.bundling.*
import javax.inject.Inject

internal open class CurseForgeArtifactNotationParser @Inject constructor(
    private val fileResolver: FileResolver,
    private val objectFactory: ObjectFactory
) {

    @Suppress("UNCHECKED_CAST")
    fun parse(any: Any): CurseForgeArtifactWrapper = when (any) {
        is AbstractArchiveTask -> parseArchiveTaskNotation(any)
        is Provider<*> -> parseProviderNotation(any as Provider<out AbstractArchiveTask>)
        is PublishArtifact -> parsePublishArtifactNotation(any)
        else -> parseFileNotation(any) ?: error("Failed to parse artifact notation: $any")
    }

    private fun parseArchiveTaskNotation(archiveTask: AbstractArchiveTask): CurseForgeArtifactWrapper =
        objectFactory.newInstance(ArchiveTaskBasedCurseForgeArtifactWrapper::class.java, archiveTask)

    private fun parseFileNotation(notation: Any): CurseForgeArtifactWrapper? {
        val file = runCatching { fileResolver.asNotationParser().parseNotation(notation) }.getOrNull() ?: return null
        val buildable = if (notation is TaskDependencyContainer) notation else null
        val artifact = objectFactory.newInstance(FileBasedCurseForgeArtifactWrapper::class.java, file, buildable)

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

    private fun parseProviderNotation(provider: Provider<out AbstractArchiveTask>): CurseForgeArtifactWrapper =
        objectFactory.newInstance(LazyCurseForgeArtifactWrapper::class.java, provider)

    private fun parsePublishArtifactNotation(publishArtifact: PublishArtifact): CurseForgeArtifactWrapper =
        objectFactory.newInstance(PublishArtifactBasedCurseForgeArtifactWrapper::class.java, publishArtifact)

}