/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.test.TestMetadata
import java.io.File

/**
 * Utility functions for generated test classes.
 *
 * These functions are used by auto-generated test classes to verify
 * that all test data files have corresponding test methods.
 */

/**
 * Verifies that all test data files in the current test class's directory
 * have corresponding generated test methods.
 */
fun assertAllFilesPresentByMetadata(testInstance: Any, testDataRootRelativePath: String) {
    val testDataDir = resolveTestDataPath(testDataRootRelativePath)
    require(testDataDir.isDirectory) { "testData dir not found: ${testDataDir.path}" }

    val currentDir = currentClassTestDataDir(testInstance::class.java, testDataDir)
    val expected = currentDir.listFiles().orEmpty().asSequence()
        .filter { it.isGeneratedTestDataFile() }
        .map { it.relativeTo(currentDir).invariantSeparatorsPath }
        .toSet()

    val covered = collectCoveredRelativePaths(testInstance::class.java, currentDir)
    val missing = expected - covered
    check(missing.isEmpty()) {
        "Missing generated tests for testData files in ${currentDir.path}: ${missing.sorted()}"
    }
}

/**
 * Resolves a test data path, searching upward from the current working directory
 * if the path is relative and doesn't exist directly.
 */
fun resolveTestDataPath(path: String): File {
    val direct = File(path)
    if (direct.isAbsolute) return direct
    if (direct.exists()) return direct

    var cursor = File(System.getProperty("user.dir", ".")).absoluteFile
    while (true) {
        val candidate = cursor.resolve(path)
        if (candidate.exists()) return candidate
        val parent = cursor.parentFile ?: break
        cursor = parent
    }
    return direct
}

/**
 * Determines the test data directory for a given test class based on its @TestMetadata annotation.
 */
fun currentClassTestDataDir(testClass: Class<*>, rootTestDataDir: File): File {
    val classMetadata = testClass.getAnnotation(TestMetadata::class.java)
    if (classMetadata != null) {
        val metadataPath = classMetadata.value.replace('\\', '/')
        val candidate = File(metadataPath)
        if (candidate.isDirectory) return candidate
        val nestedCandidate = rootTestDataDir.resolve(metadataPath)
        if (nestedCandidate.isDirectory) return nestedCandidate
    }
    return rootTestDataDir
}

/**
 * Collects all relative paths of test data files covered by test methods in a class hierarchy.
 */
fun collectCoveredRelativePaths(rootClass: Class<*>, testDataDir: File): Set<String> {
    val covered = linkedSetOf<String>()
    collectCoveredFromClass(rootClass, testDataDir, testDataDir, covered)
    return covered
}

private fun collectCoveredFromClass(
    klass: Class<*>,
    rootTestDataDir: File,
    inheritedDir: File,
    covered: MutableSet<String>,
) {
    val classScopedDir = classScopedDir(klass, rootTestDataDir, inheritedDir)
    for (method in klass.declaredMethods) {
        val metadata = method.getAnnotation(TestMetadata::class.java) ?: continue
        val metadataPath = metadata.value.replace('\\', '/')
        val candidate = classScopedDir.resolve(metadataPath)
        if (candidate.isFile && candidate.extension == "cj" && candidate.isUnder(rootTestDataDir)) {
            covered += candidate.relativeTo(rootTestDataDir).invariantSeparatorsPath
        }
    }
    for (nested in klass.declaredClasses) {
        collectCoveredFromClass(nested, rootTestDataDir, classScopedDir, covered)
    }
}

private fun classScopedDir(
    klass: Class<*>,
    rootTestDataDir: File,
    inheritedDir: File,
): File {
    val classMetadata = klass.getAnnotation(TestMetadata::class.java) ?: return inheritedDir
    val metadataPath = classMetadata.value.replace('\\', '/')
    val direct = resolveTestDataPath(metadataPath)
    if (direct.isDirectory) return direct
    val nested = rootTestDataDir.resolve(metadataPath)
    if (nested.isDirectory) return nested
    val inheritedNested = inheritedDir.resolve(metadataPath)
    if (inheritedNested.isDirectory) return inheritedNested
    return inheritedDir
}

private fun File.isUnder(parent: File): Boolean {
    val parentPath = parent.canonicalFile.toPath()
    val childPath = canonicalFile.toPath()
    return childPath.startsWith(parentPath)
}

/**
 * Determines if a file should be included as a generated test data file.
 *
 * LLT 中的 `pkg.cj` / `*.pkg.cj` 在同目录存在主测试 `.cj` 时是包 companion，
 * 只应作为同一编译单元的附加源参与主测试，不应生成独立入口测试。
 */
fun File.isGeneratedTestDataFile(): Boolean {
    if (!isFile || extension != "cj") return false
    if (!isPackageCompanionName()) return true
    val directory = parentFile ?: return true
    return directory.listFiles().orEmpty().none { sibling ->
        sibling.isFile && sibling.extension == "cj" && sibling != this && !sibling.isPackageCompanionName()
    }
}

fun File.isPackageCompanionName(): Boolean =
    name == "pkg.cj" || name.endsWith(".pkg.cj")