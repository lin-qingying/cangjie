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
 * This source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute it,
 * provided they adhere to the terms of the license.
 */

package org.cangnova.cangjie.psi.test

import org.cangnova.cangjie.test.TestMetadata
import java.io.File

/**
 * 生成式 PSI 测试（tests-gen）的公共工具。
 *
 * 对齐 analysis-tests / psi2cfir 的 GeneratedTestUtils 约定：
 * - 测试数据路径相对仓库根解析（测试任务 workingDir = rootDir）；
 * - golden sidecar 比对支持 `-Dupdate.test.data=true` 更新；
 * - `assertAllFilesPresentByMetadata` 保证 testData 下每个 `.cj` 都有生成入口。
 */

/**
 * 将测试数据路径解析为真实文件。
 *
 * 相对路径先按当前工作目录解析，不存在时逐级向上查找仓库根。
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
 * 将 [actual] 与 golden 文件比对；不一致或缺失时给出可操作的报错。
 *
 * 传入 `-Dupdate.test.data=true` 时直接重写 golden 文件，便于批量生成或接受预期变化。
 */
fun assertEqualsToFile(expectedFile: File, actual: String) {
    val actualTrimmed = actual.trim()
    val updateMode = java.lang.Boolean.getBoolean(UPDATE_TEST_DATA_PROPERTY)
    if (!expectedFile.exists()) {
        if (updateMode) {
            expectedFile.parentFile.mkdirs()
            expectedFile.writeText(actualTrimmed + "\n", Charsets.UTF_8)
            throw AssertionError("Golden file created: ${expectedFile.path}\nPlease verify and re-run.")
        }
        throw AssertionError(
            "Golden file missing: ${expectedFile.path}\n" +
                "Run with -D$UPDATE_TEST_DATA_PROPERTY=true to create golden files.\n" +
                "=== Actual ===\n$actualTrimmed",
        )
    }
    val expected = expectedFile.readText(Charsets.UTF_8).replace("\r\n", "\n").trim()
    if (expected != actualTrimmed) {
        if (updateMode) {
            expectedFile.writeText(actualTrimmed + "\n", Charsets.UTF_8)
        }
        throw AssertionError(
            "Golden file mismatch: ${expectedFile.path}\n" +
                if (updateMode) {
                    "File updated. Re-run to verify.\n"
                } else {
                    "Run with -D$UPDATE_TEST_DATA_PROPERTY=true to update golden files.\n"
                } +
                "=== Expected ===\n$expected\n=== Actual ===\n$actualTrimmed",
        )
    }
}

/**
 * 校验测试类（含 @Nested 嵌套类）覆盖了测试数据目录下全部 `.cj` 文件。
 *
 * 由生成器在每个测试类中调用；testData 新增文件但未重新生成测试入口时会失败。
 */
fun assertAllFilesPresentByMetadata(testInstance: Any, testDataRootRelativePath: String) {
    val testDataDir = resolveTestDataPath(testDataRootRelativePath)
    require(testDataDir.isDirectory) { "testData dir not found: ${testDataDir.path}" }

    val currentDir = currentClassTestDataDir(testInstance::class.java, testDataDir)
    val expected = currentDir.listFiles().orEmpty().asSequence()
        .filter { it.isGeneratedTestDataFile() }
        .map { it.relativeTo(currentDir).invariantSeparatorsPath }
        .toSet()

    val covered = collectCoveredRelativePaths(testInstance::class.java, testDataDir)
    val missing = expected - covered
    check(missing.isEmpty()) {
        "Missing generated tests for testData files in ${currentDir.path}: ${missing.sorted()}"
    }
}

/** 生成式测试数据文件：`.cj`、`.cj.d` 声明文件或 `.cdoc` 文档注释；golden sidecar 等附属文件不生成入口。 */
fun File.isGeneratedTestDataFile(): Boolean =
    isFile && (extension == "cj" || name.endsWith(".cj.d") || extension == "cdoc")

/**
 * 根据类的 @TestMetadata 计算其对应的测试数据目录。
 */
private fun currentClassTestDataDir(testClass: Class<*>, rootTestDataDir: File): File {
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
 * 递归收集测试类及其嵌套类中 @TestMetadata 覆盖的 `.cj` 文件。
 */
private fun collectCoveredRelativePaths(rootClass: Class<*>, testDataDir: File): Set<String> {
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
        if (candidate.isFile && candidate.isGeneratedTestDataFile() && candidate.isUnder(rootTestDataDir)) {
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

/** golden 更新模式系统属性名。 */
const val UPDATE_TEST_DATA_PROPERTY: String = "update.test.data"
