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
package io.github.themrmilchmann.gradle.publish.curseforge.plugins

import org.gradle.api.Task
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.TaskInternal
import org.gradle.api.internal.file.FileCollectionFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.TaskExecuter
import org.gradle.api.internal.tasks.TaskStateInternal
import org.gradle.api.internal.tasks.execution.DefaultTaskExecutionContext
import org.gradle.api.internal.tasks.properties.DefaultTaskProperties
import org.gradle.api.internal.tasks.properties.PropertyWalker
import org.gradle.execution.ProjectExecutionServices
import org.gradle.internal.execution.BuildOutputCleanupRegistry
import org.gradle.internal.execution.WorkValidationContext
import org.gradle.internal.execution.impl.DefaultWorkValidationContext
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testfixtures.internal.ProjectBuilderImpl
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir
import spock.util.environment.RestoreSystemProperties

@RestoreSystemProperties
abstract class AbstractProjectBuilderSpec extends Specification {

    @TempDir
    @Shared
    File tempDir;

    protected final DocumentationRegistry documentationRegistry = new DocumentationRegistry()

    ProjectInternal project;
    ProjectExecutionServices executionServices;

    def setup() {
        project = ProjectBuilder.builder().withProjectDir(tempDir).build() as ProjectInternal
        executionServices = new ProjectExecutionServices(project)
    }

    def cleanup() {
        ProjectBuilderImpl.stop(project)
    }

    void execute(Task task) {
        def taskExecutionContext = new DefaultTaskExecutionContext(
            null,
            DefaultTaskProperties.resolve(executionServices.get(PropertyWalker), executionServices.get(FileCollectionFactory), task as TaskInternal),
            new DefaultWorkValidationContext(documentationRegistry, WorkValidationContext.TypeOriginInspector.NO_OP),
            { historyMaintained, context -> }
        )
        project.gradle.services.get(BuildOutputCleanupRegistry).resolveOutputs()
        executionServices.get(TaskExecuter).execute((TaskInternal) task, (TaskStateInternal) task.state, taskExecutionContext)
        task.state.rethrowFailure()
    }

}