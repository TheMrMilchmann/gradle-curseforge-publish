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
package com.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts

import com.github.themrmilchmann.gradle.publish.curseforge.*
import org.gradle.api.internal.tasks.*
import org.gradle.api.tasks.*

internal abstract class AbstractCurseForgeArtifact : CurseForgeArtifact {

    private val allBuildDependencies = CompositeTaskDependency()
    private val additionalBuildDependencies = DefaultTaskDependency()

    override lateinit var changelog: Changelog
    override var displayName: String? = null
    override lateinit var releaseType: ReleaseType

    override fun builtBy(vararg tasks: Any) {
        additionalBuildDependencies.add(tasks)
    }

    override fun getBuildDependencies(): TaskDependency =
        allBuildDependencies

    abstract fun getDefaultBuildDependencies(): TaskDependency

    private inner class CompositeTaskDependency : AbstractTaskDependency() {

        override fun visitDependencies(context: TaskDependencyResolveContext) {
            context.add(getDefaultBuildDependencies())
            additionalBuildDependencies.visitDependencies(context)
        }

    }

}