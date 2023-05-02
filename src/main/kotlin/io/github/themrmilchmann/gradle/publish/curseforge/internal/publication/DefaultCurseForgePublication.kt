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
package io.github.themrmilchmann.gradle.publish.curseforge.internal.publication

import io.github.themrmilchmann.gradle.publish.curseforge.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts.*
import io.github.themrmilchmann.gradle.publish.curseforge.internal.model.api.GameDependency
import io.github.themrmilchmann.gradle.publish.curseforge.internal.model.api.GameVersion
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.internal.*
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.internal.attributes.*
import org.gradle.api.internal.component.*
import org.gradle.api.internal.file.*
import org.gradle.api.internal.tasks.TaskDependencyFactory
import org.gradle.api.model.*
import org.gradle.api.provider.*
import org.gradle.api.provider.Provider
import org.gradle.api.publish.internal.*
import org.gradle.api.publish.internal.PublicationInternal.PublishedFile
import org.gradle.api.publish.internal.versionmapping.*
import org.gradle.api.tasks.*
import org.gradle.internal.*
import org.gradle.internal.reflect.Instantiator
import org.gradle.kotlin.dsl.*
import javax.inject.*

internal open class DefaultCurseForgePublication @Inject constructor(
    private val name: String,
    private val artifactFactory: CurseForgeArtifactNotationParser,
    instantiator: Instantiator,
    fileCollectionFactory: FileCollectionFactory,
    objects: ObjectFactory,
    collectionCallbackActionDecorator: CollectionCallbackActionDecorator,
    private val taskDependencyFactory: TaskDependencyFactory
) : CurseForgePublicationInternal {

    override val projectID: Property<Int> = objects.property(Int::class.java)

    private var _mainArtifact: AbstractCurseForgeArtifact? = null
        set(value) {
            mainArtifactDomainObjectSet.clear()
            mainArtifactDomainObjectSet.add(value!!)

            field = value
        }

    override val mainArtifact: AbstractCurseForgeArtifact
        get() = _mainArtifact!!

    override val extraArtifacts: Set<AbstractCurseForgeArtifact> = LinkedHashSet()

    override fun artifact(artifact: Any): Unit =
        artifact(artifact) {}

    override fun artifact(action: Action<CurseForgeArtifact>) {
        action.execute(mainArtifact)
    }

    override fun artifact(artifact: Any, action: Action<CurseForgeArtifact>) {
        val artifactImpl = artifactFactory.parse(artifact)
        action.execute(artifactImpl)

        _mainArtifact = artifactImpl
    }

    private val gameVersionFilters = mutableListOf<(String, String) -> Boolean>()

    override fun includeGameVersions(filter: (type: String, version: String) -> Boolean) {
        gameVersionFilters += filter
    }

    override fun isGameVersionIncluded(dependency: GameDependency, version: GameVersion): Boolean =
        gameVersionFilters.any { filter ->
            if (dependency.slug == "java" && javaVersions.any { "java-${if (it is Provider<*>) it.get() else it}" == version.slug }) return true
            filter(dependency.slug, version.slug)
        }

    private val _javaVersions = mutableSetOf<Any>()
    override var javaVersions: Set<Any>
        get() = _javaVersions
        set(value) {
            _javaVersions.clear()
            _javaVersions.addAll(value)
        }

    override fun javaVersion(version: Provider<String>) {
        _javaVersions += version
    }

    override fun javaVersion(version: String) {
        _javaVersions += version
    }

    override fun javaVersions(vararg versions: String) {
        _javaVersions.addAll(versions)
    }

    override lateinit var publicationMetadataGenerator: TaskProvider<out Task>

    override fun getName(): String = name

    private var isAlias: Boolean = false
    override fun isAlias(): Boolean = isAlias
    override fun setAlias(alias: Boolean) { isAlias = alias }

    private var withBuildIdentifier: Boolean = false
    override fun withoutBuildIdentifier() { withBuildIdentifier = false }
    override fun withBuildIdentifier() { withBuildIdentifier = true }


    override fun getDisplayName(): DisplayName =
        Describables.withTypeAndName("CurseForge publication", name)

    override fun getCoordinates(): ModuleVersionIdentifier {
        return DefaultModuleVersionIdentifier.newId("com.example", "stub", "0") // TODO Can we figure out a way to properly derive this?
    }

    override fun <T : Any?> getCoordinates(type: Class<T>): T? =
        if (type.isAssignableFrom(ModuleVersionIdentifier::class.java)) {
            type.cast(coordinates)
        } else {
            null
        }

    override fun getComponent(): SoftwareComponentInternal? = null
    override fun isLegacy(): Boolean = false
    override fun getAttributes(): ImmutableAttributes = ImmutableAttributes.EMPTY

    private val mainArtifactDomainObjectSet = instantiator.newInstance(CurseForgeArtifactSet::class.java, name, fileCollectionFactory, collectionCallbackActionDecorator)
    private val derivedArtifacts = DefaultPublicationArtifactSet(CurseForgeArtifact::class.java, "derived artifacts for $name", fileCollectionFactory, collectionCallbackActionDecorator)
    private val publishableArtifacts = CompositePublicationArtifactSet(taskDependencyFactory, CurseForgeArtifact::class.java, mainArtifactDomainObjectSet, derivedArtifacts)

    override fun getPublishableArtifacts(): PublicationArtifactSet<CurseForgeArtifact> = publishableArtifacts
    override fun allPublishableArtifacts(action: Action<in CurseForgeArtifact>) { publishableArtifacts.all(action) }
    override fun whenPublishableArtifactRemoved(action: Action<in CurseForgeArtifact>) { publishableArtifacts.whenObjectRemoved { action.execute(this) } }

    override fun addDerivedArtifact(originalArtifact: CurseForgeArtifact, file: PublicationInternal.DerivedArtifact): CurseForgeArtifact {
        val artifact = DerivedCurseForgeArtifact(originalArtifact as AbstractCurseForgeArtifact, file, taskDependencyFactory)
        derivedArtifacts.add(artifact)

        return artifact
    }

    override fun removeDerivedArtifact(artifact: CurseForgeArtifact) {
        derivedArtifacts.remove(artifact)
    }

    override fun getPublishedFile(source: PublishArtifact?): PublishedFile {
        return object : PublishedFile {
            override fun getName(): String = mainArtifact.file.name
            override fun getUri(): String = "" // TODO Can we figure out a way to properly derive this?
        }
    }

    override fun getVersionMappingStrategy(): VersionMappingStrategyInternal? = null
    override fun isPublishBuildId(): Boolean = false

}