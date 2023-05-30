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
package io.github.themrmilchmann.gradle.publish.curseforge.tasks

import io.github.themrmilchmann.gradle.publish.curseforge.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.model.api.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.publication.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.utils.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.gradle.api.*
import org.gradle.api.model.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.work.*
import java.io.*
import javax.inject.*

@DisableCachingByDefault(because = "Not made cacheable, yet")
public open class GeneratePublicationMetadata @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {

    @get:Internal
    public val publication: Property<CurseForgePublication> = objects.property(CurseForgePublication::class.java)

    @TaskAction
    public fun generate() {
        val publication = publication.finalizeAndGet() as CurseForgePublicationInternal

        val json = Json {
            prettyPrint = true
        }

        generateArtifactMetadata(json, publication.mainArtifact)
        publication.extraArtifacts.forEach { artifact -> generateArtifactMetadata(json, artifact) }
    }

    private fun generateArtifactMetadata(json: Json, artifact: AbstractCurseForgeArtifact) {
        val metadata = UploadMetadata(
            changelog = artifact.changelog.content,
            changelogType = artifact.changelog.type.toJSONType(),
            displayName = artifact.displayName,
            releaseType = artifact.releaseType.toJSONType(),
            relations = artifact.relations.mapNotNull { artifactRelation ->
                artifactRelation.type.toModelType().let {
                    UploadMetadata.Relation(
                        slug = artifactRelation.slug,
                        type = it
                    )
                }
            }
        )

        val outputFile = File("${artifact.file.absolutePath}.metadata.json")
        outputFile.writeText(json.encodeToString(metadata))
    }

    private fun ArtifactRelation.Type.toModelType(): UploadMetadata.Relation.Type = when (this) {
        ArtifactRelation.Type.EmbeddedLibrary -> UploadMetadata.Relation.Type.EMBEDDED_LIBRARY
        ArtifactRelation.Type.Incompatible -> UploadMetadata.Relation.Type.INCOMPATIBLE
        ArtifactRelation.Type.OptionalDependency -> UploadMetadata.Relation.Type.OPTIONAL_DEPENDENCY
        ArtifactRelation.Type.RequiredDependency -> UploadMetadata.Relation.Type.REQUIRED_DEPENDENCY
        ArtifactRelation.Type.Tool -> UploadMetadata.Relation.Type.TOOL
    }

    private fun ChangelogType.toJSONType(): String = when (this) {
        ChangelogType.HTML -> "html"
        ChangelogType.MARKDOWN -> "markdown"
        ChangelogType.TEXT -> "text"
    }

    private fun ReleaseType.toJSONType(): String = when (this) {
        ReleaseType.ALPHA -> "alpha"
        ReleaseType.BETA -> "beta"
        ReleaseType.RELEASE -> "release"
    }

}