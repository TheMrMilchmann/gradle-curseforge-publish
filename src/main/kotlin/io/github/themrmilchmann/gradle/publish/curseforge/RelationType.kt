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
package io.github.themrmilchmann.gradle.publish.curseforge

/**
 * The type of relation between two CurseForge projects.
 *
 * The type describes how another project relates to a "current" project. See [CurseForgePublicationArtifact.relations]
 * for more information.
 *
 * @since   0.5.0
 *
 * @author  Leon Linhart
 */
public enum class RelationType {
    /**
     * Indicates that the related project is embedded into this project.
     *
     * @since   0.5.0
     */
    EMBEDDED_LIBRARY,

    /**
     * Indicates that the related project is incompatible with this project.
     *
     * @since   0.5.0
     */
    INCOMPATIBLE,

    /**
     * Indicates that the related project is an optional dependency of this project.
     *
     * @since   0.5.0
     */
    OPTIONAL_DEPENDENCY,

    /**
     * Indicates that the related project is a required dependency of this project.
     *
     * @since   0.5.0
     */
    REQUIRED_DEPENDENCY,

    /**
     * Indicates that the related project is a tool used for creating this project.
     *
     * @since   0.5.0
     */
    TOOL
}