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

import java.io.File

/**
 * Test generator for CFIR analysis diagnostics tests on the new test framework.
 *
 * It scans diagnostics test roots and generates nested suites with recursive
 * directory support.
 */
object TestGeneratorForCfirAnalysisTests {
    @JvmStatic
    fun main(args: Array<String>) {
        val projectRoot = if (args.isNotEmpty()) File(args[0]) else File(System.getProperty("user.dir"))
        generateDiagnosticsSuite(
            projectRoot = projectRoot,
            relativeTestDataRoot = "cfir/analysis-tests/testData/diagnostics",
            outputRelativePath = "cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisDiagnosticsTestGenerated.kt",
            generatedClassName = "CfirAnalysisDiagnosticsTestGenerated",
        )
        generateDiagnosticsSuite(
            projectRoot = projectRoot,
            relativeTestDataRoot = "cfir/analysis-tests/testData/diagnostics",
            outputRelativePath = "cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisDiagnosticsWithoutAliasExpansionTestGenerated.kt",
            generatedClassName = "CfirAnalysisDiagnosticsWithoutAliasExpansionTestGenerated",
            baseClassName = "AbstractCfirLightTreeDiagnosticsWithoutAliasExpansionTest",
        )
        generateDiagnosticsSuite(
            projectRoot = projectRoot,
            relativeTestDataRoot = "cfir/analysis-tests/testData/diagnostics",
            outputRelativePath = "cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisDiagnosticsPsiTestGenerated.kt",
            generatedClassName = "CfirAnalysisDiagnosticsPsiTestGenerated",
            baseClassName = "AbstractCfirPsiDiagnosticTest",
        )
        generateDiagnosticsSuite(
            projectRoot = projectRoot,
            relativeTestDataRoot = "cfir/analysis-tests/testData/diagnostics2",
            outputRelativePath = "cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisDiagnostics2TestGenerated.kt",
            generatedClassName = "CfirAnalysisDiagnostics2TestGenerated",
        )
        generateDiagnosticsSuite(
            projectRoot = projectRoot,
            relativeTestDataRoot = "cfir/analysis-tests/testData/diagnostics2",
            outputRelativePath = "cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisDiagnostics2WithoutAliasExpansionTestGenerated.kt",
            generatedClassName = "CfirAnalysisDiagnostics2WithoutAliasExpansionTestGenerated",
            baseClassName = "AbstractCfirLightTreeDiagnosticsWithoutAliasExpansionTest",
        )
        generateDiagnosticsSuite(
            projectRoot = projectRoot,
            relativeTestDataRoot = "cfir/analysis-tests/testData/diagnostics2",
            outputRelativePath = "cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisDiagnostics2PsiTestGenerated.kt",
            generatedClassName = "CfirAnalysisDiagnostics2PsiTestGenerated",
            baseClassName = "AbstractCfirPsiDiagnosticTest",
        )
        generateDiagnosticsSuite(
            projectRoot = projectRoot,
            relativeTestDataRoot = "cfir/analysis-tests/testData/llt",
            outputRelativePath = "cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisLLTTestGenerated.kt",
            generatedClassName = "CfirAnalysisLLTTestGenerated",
            baseClassName = "AbstractCfirLightTreeLlTDiagnosticsTest",

        )
        generateDiagnosticsSuite(
            projectRoot = projectRoot,
            relativeTestDataRoot = "cfir/analysis-tests/testData/llt",
            outputRelativePath = "cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisLLTPsiTestGenerated.kt",
            generatedClassName = "CfirAnalysisLLTPsiTestGenerated",
            baseClassName = "AbstractCfirPsiLlTDiagnosticsTest",


        )
        generateDiagnosticsSuite(
            projectRoot = projectRoot,
            relativeTestDataRoot = "cfir/analysis-tests/testData/macro",
            outputRelativePath = "cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisMacroTestGenerated.kt",
            generatedClassName = "CfirAnalysisMacroTestGenerated",
            baseClassName = "AbstractCfirLightTreeMacroDiagnosticsTest",
        )
        generateDiagnosticsSuite(
            projectRoot = projectRoot,
            relativeTestDataRoot = "cfir/analysis-tests/testData/macro",
            outputRelativePath = "cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisMacroPsiTestGenerated.kt",
            generatedClassName = "CfirAnalysisMacroPsiTestGenerated",
            baseClassName = "AbstractCfirPsiMacroDiagnosticsTest",
        )
    }

    private fun generateDiagnosticsSuite(
        projectRoot: File,
        relativeTestDataRoot: String,
        outputRelativePath: String,
        generatedClassName: String,
        baseClassName: String = "AbstractCfirLightTreeDiagnosticsTest",
    ) {
        val testDataRoot = projectRoot.resolve(relativeTestDataRoot)
        require(testDataRoot.exists()) { "testData root not found: ${testDataRoot.path}" }

        val rootRel = projectRoot.toPath().relativize(testDataRoot.toPath()).toString().replace('\\', '/')

        val rootFiles = testDataRoot.listFiles().orEmpty()
            .filter { it.isFile && it.extension == "cj" }
            .sortedBy { it.name }

        val subDirs = testDataRoot.listFiles().orEmpty()
            .filter { it.isDirectory }
            .sortedBy { it.name }

        val outputFile = projectRoot.resolve(outputRelativePath)
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            renderSuite(
                rootRel = rootRel,
                rootFiles = rootFiles,
                subDirs = subDirs,
                projectRoot = projectRoot,
                generatedClassName = generatedClassName,
                baseClassName = baseClassName,
            ),
            Charsets.UTF_8,
        )
        println("Generated: ${outputFile.path}")
    }

    private fun renderSuite(
        rootRel: String,
        rootFiles: List<File>,
        subDirs: List<File>,
        projectRoot: File,
        generatedClassName: String,
        baseClassName: String,
    ): String = buildString {
        appendLine("package org.cangnova.cangjie.cfir.analysis.tests")
        appendLine()
        appendLine("import com.intellij.testFramework.TestDataPath")
        appendLine("import org.cangnova.cangjie.ObsoleteTestInfrastructure")
        appendLine("import org.cangnova.cangjie.cfir.analysis.tests.runners.$baseClassName")
        appendLine("import org.cangnova.cangjie.test.TestMetadata")
        appendLine("import org.junit.jupiter.api.Nested")
        appendLine("import org.junit.jupiter.api.Test")
        appendLine("import java.io.File")
        appendLine()
        appendLine("/** AUTO-GENERATED by TestGeneratorForCfirAnalysisTests. DO NOT EDIT MANUALLY. */")
        appendLine("@TestMetadata(\"$rootRel\")")
        appendLine("@TestDataPath(\"\\${'$'}PROJECT_ROOT\")")
        appendLine("@OptIn(ObsoleteTestInfrastructure::class)")
        appendLine("@ObsoleteTestInfrastructure")
        appendLine("class $generatedClassName : $baseClassName() {")
        appendLine("    @Test")
        appendLine("    fun testAllFilesPresent() {")
        appendLine("        assertAllFilesPresentByMetadata(this, \"$rootRel\")")
        appendLine("    }")

        for (file in rootFiles) {
            appendLine()
            appendTestMethod(file, projectRoot, indent = "    ")
        }

        for (dir in subDirs) {
            appendDirectoryClass(
                dir = dir,
                rootRel = rootRel,
                projectRoot = projectRoot,
                indent = "    ",
                relativePathFromRoot = dir.name,
                baseClassName = baseClassName,
            )
        }

        appendLine("}")
        appendLine()
        appendLine("private fun assertAllFilesPresentByMetadata(testInstance: Any, testDataRootRelativePath: String) {")
        appendLine("    val testDataDir = resolveTestDataPath(testDataRootRelativePath)")
        appendLine("    require(testDataDir.isDirectory) { \"testData dir not found: \${testDataDir.path}\" }")
        appendLine()
        appendLine("    val currentDir = currentClassTestDataDir(testInstance::class.java, testDataDir)")
        appendLine("    val expected = currentDir.listFiles().orEmpty().asSequence()")
        appendLine("        .filter { it.isFile && it.extension == \"cj\" }")
        appendLine("        .map { it.relativeTo(currentDir).invariantSeparatorsPath }")
        appendLine("        .toSet()")
        appendLine()
        appendLine("    val covered = collectCoveredRelativePaths(testInstance::class.java, currentDir)")
        appendLine("    val missing = expected - covered")
        appendLine("    check(missing.isEmpty()) {")
        appendLine("        \"Missing generated tests for testData files in \${currentDir.path}: \${missing.sorted()}\"")
        appendLine("    }")
        appendLine("}")
        appendLine()
        appendLine("private fun resolveTestDataPath(path: String): File {")
        appendLine("    val direct = File(path)")
        appendLine("    if (direct.isAbsolute) return direct")
        appendLine("    if (direct.exists()) return direct")
        appendLine()
        appendLine("    var cursor = File(System.getProperty(\"user.dir\", \".\")).absoluteFile")
        appendLine("    while (true) {")
        appendLine("        val candidate = cursor.resolve(path)")
        appendLine("        if (candidate.exists()) return candidate")
        appendLine("        val parent = cursor.parentFile ?: break")
        appendLine("        cursor = parent")
        appendLine("    }")
        appendLine("    return direct")
        appendLine("}")
        appendLine()
        appendLine("private fun currentClassTestDataDir(testClass: Class<*>, rootTestDataDir: File): File {")
        appendLine("    val classMetadata = testClass.getAnnotation(org.cangnova.cangjie.test.TestMetadata::class.java)")
        appendLine("    if (classMetadata != null) {")
        appendLine("        val metadataPath = classMetadata.value.replace('\\\\', '/')")
        appendLine("        val candidate = File(metadataPath)")
        appendLine("        if (candidate.isDirectory) return candidate")
        appendLine("        val nestedCandidate = rootTestDataDir.resolve(metadataPath)")
        appendLine("        if (nestedCandidate.isDirectory) return nestedCandidate")
        appendLine("    }")
        appendLine("    return rootTestDataDir")
        appendLine("}")
        appendLine()
        appendLine("private fun collectCoveredRelativePaths(rootClass: Class<*>, testDataDir: File): Set<String> {")
        appendLine("    val covered = linkedSetOf<String>()")
        appendLine("    collectCoveredFromClass(rootClass, testDataDir, testDataDir, covered)")
        appendLine("    return covered")
        appendLine("}")
        appendLine()
        appendLine("private fun collectCoveredFromClass(")
        appendLine("    klass: Class<*>,")
        appendLine("    rootTestDataDir: File,")
        appendLine("    inheritedDir: File,")
        appendLine("    covered: MutableSet<String>,")
        appendLine(") {")
        appendLine("    val classScopedDir = classScopedDir(klass, rootTestDataDir, inheritedDir)")
        appendLine("    for (method in klass.declaredMethods) {")
        appendLine("        val metadata = method.getAnnotation(org.cangnova.cangjie.test.TestMetadata::class.java) ?: continue")
        appendLine("        val metadataPath = metadata.value.replace('\\\\', '/')")
        appendLine("        val candidate = classScopedDir.resolve(metadataPath)")
        appendLine("        if (candidate.isFile && candidate.extension == \"cj\" && candidate.isUnder(rootTestDataDir)) {")
        appendLine("            covered += candidate.relativeTo(rootTestDataDir).invariantSeparatorsPath")
        appendLine("        }")
        appendLine("    }")
        appendLine("    for (nested in klass.declaredClasses) {")
        appendLine("        collectCoveredFromClass(nested, rootTestDataDir, classScopedDir, covered)")
        appendLine("    }")
        appendLine("}")
        appendLine()
        appendLine("private fun classScopedDir(")
        appendLine("    klass: Class<*>,")
        appendLine("    rootTestDataDir: File,")
        appendLine("    inheritedDir: File,")
        appendLine("): File {")
        appendLine("    val classMetadata = klass.getAnnotation(org.cangnova.cangjie.test.TestMetadata::class.java) ?: return inheritedDir")
        appendLine("    val metadataPath = classMetadata.value.replace('\\\\', '/')")
        appendLine("    val direct = resolveTestDataPath(metadataPath)")
        appendLine("    if (direct.isDirectory) return direct")
        appendLine("    val nested = rootTestDataDir.resolve(metadataPath)")
        appendLine("    if (nested.isDirectory) return nested")
        appendLine("    val inheritedNested = inheritedDir.resolve(metadataPath)")
        appendLine("    if (inheritedNested.isDirectory) return inheritedNested")
        appendLine("    return inheritedDir")
        appendLine("}")
        appendLine()
        appendLine("private fun File.isUnder(parent: File): Boolean {")
        appendLine("    val parentPath = parent.canonicalFile.toPath()")
        appendLine("    val childPath = canonicalFile.toPath()")
        appendLine("    return childPath.startsWith(parentPath)")
        appendLine("}")
    }

    private fun StringBuilder.appendDirectoryClass(
        dir: File,
        rootRel: String,
        projectRoot: File,
        indent: String,
        relativePathFromRoot: String,
        baseClassName: String,
    ) {
        val files = dir.listFiles().orEmpty()
            .filter { it.isFile && it.extension == "cj" }
            .sortedBy { it.name }

        val nestedDirs = dir.listFiles().orEmpty()
            .filter { it.isDirectory }
            .sortedBy { it.name }

        if (files.isEmpty() && nestedDirs.isEmpty()) return

        val className = dirNameToPascalCase(dir.name)

        appendLine()
        appendLine("${indent}@TestMetadata(\"${dir.name}\")")
        appendLine("${indent}@TestDataPath(\"\\${'$'}PROJECT_ROOT\")")
        appendLine("${indent}@Nested")
        appendLine("${indent}inner class $className : $baseClassName() {")
        appendLine("${indent}    @Test")
        appendLine("${indent}    fun testAllFilesPresent() {")
        appendLine("${indent}        assertAllFilesPresentByMetadata(this, \"$rootRel/$relativePathFromRoot\")")
        appendLine("${indent}    }")

        for (file in files) {
            appendLine()
            appendTestMethod(file, projectRoot, indent = "$indent    ")
        }

        for (nestedDir in nestedDirs) {
            appendDirectoryClass(
                dir = nestedDir,
                rootRel = rootRel,
                projectRoot = projectRoot,
                indent = "$indent    ",
                relativePathFromRoot = "$relativePathFromRoot/${nestedDir.name}",
                baseClassName = baseClassName,
            )
        }

        appendLine("${indent}}")
    }

    private fun StringBuilder.appendTestMethod(
        file: File,
        projectRoot: File,
        indent: String,
    ) {
        val testName = fileNameToTestName(file.nameWithoutExtension)
        val rel = projectRoot.toPath().relativize(file.toPath()).toString().replace('\\', '/')
        appendLine("${indent}@TestMetadata(\"${file.name}\")")
        appendLine("${indent}@Test")
        appendLine("${indent}fun $testName() {")
        appendLine("${indent}    runTest(\"$rel\")")
        appendLine("${indent}}")
    }

    private fun fileNameToTestName(nameWithoutExtension: String): String {
        return "test" + nameToPascalCase(nameWithoutExtension)
    }

    private fun dirNameToPascalCase(dirName: String): String {
        return nameToKotlinIdentifier(nameToPascalCase(dirName))
    }

    private fun nameToPascalCase(name: String): String {
        return name
            .split(Regex("[^A-Za-z0-9]+"))
            .filter { it.isNotBlank() }
            .joinToString("") { token -> token.replaceFirstChar { ch -> ch.uppercaseChar() } }
            .ifEmpty { "Generated" }
    }

    /**
     * 将测试数据目录名规整为 Kotlin 源码可直接声明的类名。
     *
     * LLT 官方用例中存在 `01_diff_file` 这类数字开头目录，生成嵌套测试类时保留原始信息并补 `_` 前缀。
     */
    private fun nameToKotlinIdentifier(name: String): String {
        val result = StringBuilder()
        for (ch in name) {
            if (ch == '_' || ch.isLetterOrDigit()) {
                result.append(ch)
            } else {
                result.append('_')
            }
        }
        if (result.isEmpty()) {
            result.append("Generated")
        }
        if (result.first().isDigit()) {
            result.insert(0, "_")
        }
        return result.toString()
    }

}
