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

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Task
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import java.io.File
import javax.inject.Inject

internal open class LazyCurseForgeArtifactWrapper @Inject constructor(
    private val provider: Provider<*>,
    private val objectFactory: ObjectFactory
) : CurseForgeArtifactWrapper {

    private val delegate: CurseForgeArtifactWrapper by lazy {
        when (val value = provider.get()) {
            is FileSystemLocation -> objectFactory.newInstance(FileBasedCurseForgeArtifactWrapper::class.java, value.asFile)
            is File -> objectFactory.newInstance(FileBasedCurseForgeArtifactWrapper::class.java, value)
            is AbstractArchiveTask -> objectFactory.newInstance(ArchiveTaskBasedCurseForgeArtifactWrapper::class.java, value)
            is Task -> objectFactory.newInstance(FileBasedCurseForgeArtifactWrapper::class.java, value.outputs.files.singleFile)
            else -> throw InvalidUserDataException("Cannot convert provided value ($value) to a file.")
        }
    }

    override val file: File
        get() = delegate.file

    override fun getBuildDependencies(): TaskDependency =
        delegate.buildDependencies

}