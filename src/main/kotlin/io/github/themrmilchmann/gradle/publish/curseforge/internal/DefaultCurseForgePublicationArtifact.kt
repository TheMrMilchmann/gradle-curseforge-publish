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
package io.github.themrmilchmann.gradle.publish.curseforge.internal

import io.github.themrmilchmann.gradle.publish.curseforge.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts.CurseForgeArtifactWrapper
import io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts.CurseForgeArtifactNotationParser
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskDependency
import java.io.File
import javax.inject.Inject

/**
 * Default implementation of [DefaultCurseForgePublicationArtifact].
 *
 * @author  Leon Linhart
 */
internal open class DefaultCurseForgePublicationArtifact @Inject constructor(
    override val name: String,
    private val artifactFactory: CurseForgeArtifactNotationParser,
    private val objectFactory: ObjectFactory,
    providerFactory: ProviderFactory,
    project: Project
) : CurseForgePublicationArtifactInternal {

    override val displayName: Property<String> = objectFactory.property(String::class.java)
        .convention(providerFactory.provider { "${project.name} ${project.version}" })

    override val releaseType: Property<ReleaseType> = objectFactory.property(ReleaseType::class.java)
        .convention(ReleaseType.RELEASE)

    override val changelog: Changelog = objectFactory.newInstance(Changelog::class.java)

    override val relations: ArtifactRelations = object : ArtifactRelations, MutableSet<ArtifactRelation> by mutableSetOf() {
        override fun add(type: RelationType, slug: String) {
            add(ArtifactRelation(slug, type))
        }
    }

    private var _artifactWrapper: CurseForgeArtifactWrapper? = null

    private val artifactWrapper: CurseForgeArtifactWrapper
        get() = _artifactWrapper ?: throw IllegalStateException("Artifact source has not been configured for artifact \"$name\"")

    override val file: File
        get() = artifactWrapper.file

    @get:InputFiles
    override val publishableFiles: FileCollection
        get() = objectFactory.fileCollection()
            .from(file)
            .builtBy(this)

    @Internal
    override fun getBuildDependencies(): TaskDependency =
        artifactWrapper.buildDependencies

    override fun from(file: Any) {
        _artifactWrapper = artifactFactory.parse(file)
    }

}