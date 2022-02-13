/*
 * Copyright (c) 2021-2022 Leon Linhart
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

import io.github.themrmilchmann.gradle.publish.curseforge.CurseForgePublication
import io.github.themrmilchmann.gradle.publish.curseforge.internal.publication.DefaultCurseForgePublication
import io.github.themrmilchmann.gradle.publish.curseforge.tasks.GeneratePublicationMetadata
import io.github.themrmilchmann.gradle.publish.curseforge.tasks.PublishToCurseForgeRepository
import io.github.themrmilchmann.gradle.publish.curseforge.test.fixtures.AbstractProjectBuilderSpec
import org.gradle.api.publish.PublishingExtension

class CurseForgePublishPluginTest extends AbstractProjectBuilderSpec {

    PublishingExtension publishing

    def setup() {
        project.pluginManager.apply(CurseForgePublishPlugin)
        publishing = project.extensions.getByType(PublishingExtension)
    }

    def "no publication by default"() {
        expect:
        publishing.publications.empty
    }

    def "publication can be added"() {
        when:
        publishing.publications.create("test", CurseForgePublication)

        then:
        publishing.publications.size() == 1
        publishing.publications.test instanceof DefaultCurseForgePublication
    }

    def "creates generation tasks for publication"() {
        when:
        publishing.publications.create("test", CurseForgePublication)

        then:
        project.tasks["generateMetadataFilesForTestPublication"] instanceof GeneratePublicationMetadata
    }

    def "creates publish tasks for each publication and repository"() {
        when:
        publishing.publications.create("test", CurseForgePublication)
        publishing.repositories.curseForge.repository("https://foo.com") {}

        then:
        project.tasks["publishTestPublicationToCurseForgeRepository"] instanceof PublishToCurseForgeRepository
    }

    def "tasks are created for compatible publication / repo"() {
        given:
        publishing.publications.create("test", CurseForgePublication)

        when:
        def repo1 = publishing.repositories.curseForge.repository("https://foo.com") {}
        def repo2 = publishing.repositories.curseForge.repository("https://bar.com") { name "other" }
        publishing.repositories.ivy {}

        then:
        publishTasks.size() == 2
        publishTasks.first().repository.is(repo1)
        publishTasks.first().name == "publishTestPublicationToCurseForgeRepository"
        publishTasks.last().repository.is(repo2)
        publishTasks.last().name == "publishTestPublicationToOtherRepository"
    }

    List<PublishToCurseForgeRepository> getPublishTasks() {
        def allTasks = project.tasks.withType(PublishToCurseForgeRepository).sort { it.name }
        return allTasks
    }

    def "creates publish tasks for all publications in a repository"() {
        when:
        publishing.publications.create("test", CurseForgePublication)
        publishing.publications.create("test2", CurseForgePublication)
        publishing.repositories.curseForge.repository("https://foo.com") {}
        publishing.repositories.curseForge.repository("https://bar.com") { name = "other" }

        then:
        project.tasks["publishAllPublicationsToCurseForgeRepository"].dependsOn.containsAll([
            "publishTestPublicationToCurseForgeRepository",
            "publishTest2PublicationToCurseForgeRepository"
        ])
        project.tasks["publishAllPublicationsToOtherRepository"].dependsOn.containsAll([
            "publishTestPublicationToOtherRepository",
            "publishTest2PublicationToOtherRepository"
        ])
    }

}