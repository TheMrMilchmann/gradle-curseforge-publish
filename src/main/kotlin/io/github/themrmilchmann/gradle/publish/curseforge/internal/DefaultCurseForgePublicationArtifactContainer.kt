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
package io.github.themrmilchmann.gradle.publish.curseforge.internal

import io.github.themrmilchmann.gradle.publish.curseforge.CurseForgePublicationArtifact
import io.github.themrmilchmann.gradle.publish.curseforge.CurseForgePublicationArtifactContainer
import io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts.CurseForgeArtifactNotationParser
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Default implementation of [CurseForgePublicationArtifactContainer].
 *
 * @author  Leon Linhart
 */
internal open class DefaultCurseForgePublicationArtifactContainer @Inject internal constructor(
    objects: ObjectFactory
): CurseForgePublicationArtifactContainer, NamedDomainObjectContainer<CurseForgePublicationArtifact> by objects.domainObjectContainer(CurseForgePublicationArtifact::class.java, { name ->
    val artifactFactory = objects.newInstance(CurseForgeArtifactNotationParser::class.java)
    objects.newInstance(DefaultCurseForgePublicationArtifact::class.java, name, artifactFactory)
})