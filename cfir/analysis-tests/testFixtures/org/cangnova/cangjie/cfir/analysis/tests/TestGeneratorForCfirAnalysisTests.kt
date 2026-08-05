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

    /**
     * 测试套件生成模式
     *
     * - SINGLE_CLASS: 所有测试生成在一个类中，通过嵌套类实现目录结构
     * - PER_PACKAGE: 每个子目录生成一个独立的 Kotlin 文件，包结构与目录结构对应
     */
    enum class SuiteGenerationMode {
        /** 单一类：根目录 + 嵌套类 */
        SINGLE_CLASS,
        /** 按包分布：每个子目录对应一个 Kotlin 文件和包 */
        PER_PACKAGE,
    }

    /**
     * 生成 CFIR analysis diagnostics 全部测试套件入口。
     *
     * 第一个参数可指定仓库根目录；未提供时使用当前工作目录。
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val projectRoot = if (args.isNotEmpty()) File(args[0]) else File(System.getProperty("user.dir"))
        cleanGeneratedTests(projectRoot)
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
            generationMode = SuiteGenerationMode.SINGLE_CLASS
        )
        generateDiagnosticsSuite(
            projectRoot = projectRoot,
            relativeTestDataRoot = "cfir/analysis-tests/testData/macro",
            outputRelativePath = "cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisMacroPsiTestGenerated.kt",
            generatedClassName = "CfirAnalysisMacroPsiTestGenerated",
            baseClassName = "AbstractCfirPsiMacroDiagnosticsTest",
            generationMode = SuiteGenerationMode.SINGLE_CLASS

        )
    }

    /**
     * 生成指定 testData 根目录对应的诊断测试套件。
     *
     * 该入口负责选择单文件或按包生成模式，并把根目录文件与子目录分发给具体 renderer。
     */
    private fun generateDiagnosticsSuite(
        projectRoot: File,
        relativeTestDataRoot: String,
        outputRelativePath: String,
        generatedClassName: String,
        baseClassName: String = "AbstractCfirLightTreeDiagnosticsTest",
        generationMode: SuiteGenerationMode = SuiteGenerationMode.SINGLE_CLASS,
        basePackage: String = "org.cangnova.cangjie.cfir.analysis.tests",
    ) {
        val testDataRoot = projectRoot.resolve(relativeTestDataRoot)
        require(testDataRoot.exists()) { "testData root not found: ${testDataRoot.path}" }

        val rootRel = testDataRoot.relativeTo(projectRoot).path.replace('\\', '/')

        // 从 relativeTestDataRoot 提取 testDataRoot 名称作为根包名的一部分
        // 例如: "cfir/analysis-tests/testData/diagnostics" -> "diagnostics"
        val testDataRootName = relativeTestDataRoot.substringAfterLast('/')

        val rootFiles = testDataRoot.listFiles().orEmpty()
            .filter { it.isGeneratedTestDataFile() }
            .sortedBy { it.name }

        val subDirs = testDataRoot.listFiles().orEmpty()
            .filter { it.isDirectory }
            .sortedBy { it.name }

        when (generationMode) {
            SuiteGenerationMode.SINGLE_CLASS -> generateSingleClassSuite(
                projectRoot = projectRoot,
                rootRel = rootRel,
                rootFiles = rootFiles,
                subDirs = subDirs,
                outputRelativePath = outputRelativePath,
                generatedClassName = generatedClassName,
                baseClassName = baseClassName,
                basePackage = basePackage,
            )
            SuiteGenerationMode.PER_PACKAGE -> generatePerPackageSuite(
                projectRoot = projectRoot,
                rootRel = rootRel,
                rootFiles = rootFiles,
                subDirs = subDirs,
                outputRelativePath = outputRelativePath,
                generatedClassName = generatedClassName,
                baseClassName = baseClassName,
                basePackage = basePackage,
                testDataRootName = testDataRootName,
            )
        }
    }

    /**
     * tests-gen 是本生成器的完整输出目录。每轮生成前清理旧输出，避免模式切换后
     * 过期 Kotlin 源继续进入 test source-set。
     */
    private fun cleanGeneratedTests(projectRoot: File) {
        val generatedRoot = projectRoot.resolve(
            "cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests"
        )
        if (generatedRoot.exists()) {
            generatedRoot.deleteRecursively()
        }
    }

    /**
     * SINGLE_CLASS 模式：生成单一类，所有测试方法和嵌套类都在一起
     */
    private fun generateSingleClassSuite(
        projectRoot: File,
        rootRel: String,
        rootFiles: List<File>,
        subDirs: List<File>,
        outputRelativePath: String,
        generatedClassName: String,
        baseClassName: String,
        basePackage: String,
    ) {
        val outputFile = projectRoot.resolve(outputRelativePath)
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            renderSingleClassSuite(
                rootRel = rootRel,
                rootFiles = rootFiles,
                subDirs = subDirs,
                projectRoot = projectRoot,
                generatedClassName = generatedClassName,
                baseClassName = baseClassName,
                basePackage = basePackage,
            ),
            Charsets.UTF_8,
        )
        println("Generated: ${outputFile.path}")
    }

    /**
     * PER_PACKAGE 模式：每个子目录生成一个独立的 Kotlin 文件，包结构与目录结构对应
     */
    private fun generatePerPackageSuite(
        projectRoot: File,
        rootRel: String,
        rootFiles: List<File>,
        subDirs: List<File>,
        outputRelativePath: String,
        generatedClassName: String,
        baseClassName: String,
        basePackage: String,
        testDataRootName: String,
    ) {
        // 入口点包名 = basePackage（不含 testDataRootName）
        val entryPointPackageName = basePackage

        // 子目录测试按生成套件隔离。LLT light-tree、PSI、withoutAlias 等套件会共享同一份
        // testData 根目录；子包和落盘目录必须带上套件名，避免不同基类的同名目录测试互相覆盖。
        val suiteNamespace = generatedClassName.removeSuffix("Generated").toPackageSegment()

        // 根包名 = basePackage + suiteNamespace + testDataRootName。包名段必须规整为合法 Kotlin
        // 标识符，testData 目录名则保持原样写入文件系统路径和 TestMetadata。
        val rootPackageName = "$basePackage.$suiteNamespace.${testDataRootName.toPackageSegment()}"

        // 生成根目录的主文件
        val outputDir = projectRoot.resolve(outputRelativePath).parentFile
        outputDir.mkdirs()

        val mainFile = projectRoot.resolve(outputRelativePath)
        mainFile.writeText(
            renderPerPackageRootSuite(
                rootRel = rootRel,
                rootFiles = rootFiles,
                subDirs = subDirs,
                projectRoot = projectRoot,
                generatedClassName = generatedClassName,
                baseClassName = baseClassName,
                packageName = entryPointPackageName,
            ),
            Charsets.UTF_8,
        )
        println("Generated: ${mainFile.path}")

        // 为每个子目录生成独立的包文件
        generatePerPackageSubdirs(
            projectRoot = projectRoot,
            rootRel = rootRel,
            baseOutputDir = File(File(outputDir, suiteNamespace), testDataRootName),
            baseClassName = baseClassName,
            rootPackageName = rootPackageName,
            subDirs = subDirs,
            relativePathFromRoot = "",
        )
    }

    /**
     * 递归生成 PER_PACKAGE 模式下的子目录测试文件。
     */
    private fun generatePerPackageSubdirs(
        projectRoot: File,
        rootRel: String,
        baseOutputDir: File,
        baseClassName: String,
        rootPackageName: String,
        subDirs: List<File>,
        relativePathFromRoot: String,
    ) {
        for (dir in subDirs) {
            val subClassName = "${dirNameToPascalCase(dir.name)}Generated"
            val subOutputDir = File(baseOutputDir, dir.name)
            subOutputDir.mkdirs()
            val subOutputFile = File(subOutputDir, "$subClassName.kt")

            // 子目录包名 = rootPackageName + 子目录路径
            val currentRelativePath = if (relativePathFromRoot.isEmpty()) dir.name else "$relativePathFromRoot/${dir.name}"
            val subPackageName = "$rootPackageName.${currentRelativePath.toPackagePath()}"

            subOutputFile.writeText(
                renderPerPackageDirSuite(
                    dir = dir,
                    rootRel = rootRel,
                    projectRoot = projectRoot,
                    generatedClassName = subClassName,
                    baseClassName = baseClassName,
                    packageName = subPackageName,
                    relativePathFromRoot = currentRelativePath,
                ),
                Charsets.UTF_8,
            )
            println("Generated: ${subOutputFile.path}")

            // 递归处理嵌套子目录
            val nestedSubDirs = dir.listFiles().orEmpty()
                .filter { it.isDirectory }
                .sortedBy { it.name }
            if (nestedSubDirs.isNotEmpty()) {
                generatePerPackageSubdirs(
                    projectRoot = projectRoot,
                    rootRel = rootRel,
                    baseOutputDir = subOutputDir,
                    baseClassName = baseClassName,
                    rootPackageName = rootPackageName,
                    subDirs = nestedSubDirs,
                    relativePathFromRoot = currentRelativePath,
                )
            }
        }
    }

    /**
     * SINGLE_CLASS 模式渲染函数：生成单一类，所有测试方法和嵌套类都在一起
     */
    private fun renderSingleClassSuite(
        rootRel: String,
        rootFiles: List<File>,
        subDirs: List<File>,
        projectRoot: File,
        generatedClassName: String,
        baseClassName: String,
        basePackage: String,
    ): String = buildString {
        appendLine("package $basePackage")
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
        appendLine("@TestDataPath(\"\${'$'}PROJECT_ROOT\")")
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
            appendNestedDirectoryClass(
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
    }

    /**
     * PER_PACKAGE 模式根文件渲染函数：根目录测试 + 对子目录包的引用（不使用 @Nested）
     */
    private fun renderPerPackageRootSuite(
        rootRel: String,
        rootFiles: List<File>,
        subDirs: List<File>,
        projectRoot: File,
        generatedClassName: String,
        baseClassName: String,
        packageName: String,
    ): String = buildString {
        appendLine("package $packageName")
        appendLine()
        appendLine("import com.intellij.testFramework.TestDataPath")
        appendLine("import org.cangnova.cangjie.ObsoleteTestInfrastructure")
        appendLine("import org.cangnova.cangjie.cfir.analysis.tests.runners.$baseClassName")
        appendLine("import org.cangnova.cangjie.test.TestMetadata")
        appendLine("import org.junit.jupiter.api.Test")
        appendLine("import java.io.File")
        // 导入公共 utility 函数
        appendLine("import org.cangnova.cangjie.cfir.analysis.tests.assertAllFilesPresentByMetadata")
        appendLine()
        appendLine("/** AUTO-GENERATED by TestGeneratorForCfirAnalysisTests. DO NOT EDIT MANUALLY. */")
        appendLine("@TestMetadata(\"$rootRel\")")
        appendLine("@TestDataPath(\"\${'$'}PROJECT_ROOT\")")
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

        // PER_PACKAGE 模式：子目录是独立的包/文件，不再用 @Nested
        for (dir in subDirs) {
            appendLine()
            appendPackageReference(dir, indent = "    ")
        }

        appendLine("}")
        appendLine()
    }

    /**
     * PER_PACKAGE 模式子目录包渲染函数：生成独立的 Kotlin 文件，每个子目录一个
     */
    private fun renderPerPackageDirSuite(
        dir: File,
        rootRel: String,
        projectRoot: File,
        generatedClassName: String,
        baseClassName: String,
        packageName: String,
        relativePathFromRoot: String,
    ): String = buildString {
        appendLine("package $packageName")
        appendLine()
        appendLine("import com.intellij.testFramework.TestDataPath")
        appendLine("import org.cangnova.cangjie.ObsoleteTestInfrastructure")
        appendLine("import org.cangnova.cangjie.cfir.analysis.tests.runners.$baseClassName")
        appendLine("import org.cangnova.cangjie.test.TestMetadata")
        appendLine("import org.junit.jupiter.api.Test")
        appendLine("import java.io.File")
        // 导入公共 utility 函数
        appendLine("import org.cangnova.cangjie.cfir.analysis.tests.assertAllFilesPresentByMetadata")
        appendLine()
        appendLine("/** AUTO-GENERATED by TestGeneratorForCfirAnalysisTests. DO NOT EDIT MANUALLY. */")
        appendLine("@TestMetadata(\"$rootRel/$relativePathFromRoot\")")
        appendLine("@TestDataPath(\"\${'$'}PROJECT_ROOT\")")
        appendLine("@OptIn(ObsoleteTestInfrastructure::class)")
        appendLine("@ObsoleteTestInfrastructure")
        appendLine("class $generatedClassName : $baseClassName() {")
        appendLine("    @Test")
        appendLine("    fun testAllFilesPresent() {")
        appendLine("        assertAllFilesPresentByMetadata(this, \"$rootRel/$relativePathFromRoot\")")
        appendLine("    }")

        val files = dir.listFiles().orEmpty()
            .filter { it.isGeneratedTestDataFile() }
            .sortedBy { it.name }

        for (file in files) {
            appendLine()
            appendTestMethod(file, projectRoot, indent = "    ")
        }

        // PER_PACKAGE 模式：嵌套子目录作为独立文件处理，由 generatePerPackageSubdirs 递归处理
        // 此处不再生成 @Nested 类

        appendLine("}")
        appendLine()
    }

    /**
     * 在 PER_PACKAGE 模式下输出子目录包的引用注释
     */
    private fun StringBuilder.appendPackageReference(dir: File, indent: String) {
        val className = "${dirNameToPascalCase(dir.name)}Generated"
        appendLine("${indent}// Package: ${dir.name} -> $className")
    }

    /**
     * 在 SINGLE_CLASS 模式下追加一个嵌套目录测试类。
     */
    private fun StringBuilder.appendNestedDirectoryClass(
        dir: File,
        rootRel: String,
        projectRoot: File,
        indent: String,
        relativePathFromRoot: String,
        baseClassName: String,
    ) {
        val files = dir.listFiles().orEmpty()
            .filter { it.isGeneratedTestDataFile() }
            .sortedBy { it.name }

        val nestedDirs = dir.listFiles().orEmpty()
            .filter { it.isDirectory }
            .sortedBy { it.name }

        if (files.isEmpty() && nestedDirs.isEmpty()) return

        val className = dirNameToPascalCase(dir.name)

        appendLine()
        appendLine("${indent}@TestMetadata(\"${dir.name}\")")
        appendLine("${indent}@TestDataPath(\"\${'$'}PROJECT_ROOT\")")
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
            appendNestedDirectoryClass(
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

    /**
     * 为单个 `.cj` 测试数据追加测试方法。
     */
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

    /**
     * 将文件名转换为生成测试方法名。
     */
    private fun fileNameToTestName(nameWithoutExtension: String): String {
        return "test" + nameToPascalCase(nameWithoutExtension)
    }

    /**
     * 将目录名转换为可用作 Kotlin 类名的 PascalCase。
     */
    private fun dirNameToPascalCase(dirName: String): String {
        return nameToKotlinIdentifier(nameToPascalCase(dirName))
    }

    /**
     * 将任意测试数据名称转换为 PascalCase 片段。
     */
    private fun nameToPascalCase(name: String): String {
        return name
            .split(Regex("[^A-Za-z0-9]+"))
            .filter { it.isNotBlank() }
            .joinToString("") { token -> token.replaceFirstChar { ch -> ch.uppercaseChar() } }
            .ifEmpty { "Generated" }
    }

    /**
     * 将相对目录路径转换为 Kotlin package path。
     */
    private fun String.toPackagePath(): String =
        split('/')
            .filter(String::isNotBlank)
            .joinToString(".") { segment -> segment.toPackageSegment() }

    /**
     * testData 目录名允许 `const-eval`、`class`、`if-let-expr` 这类名字；
     * 生成 Kotlin package 时必须转成合法且非关键字的标识符。
     */
    private fun String.toPackageSegment(): String {
        val normalized = buildString {
            for (ch in this@toPackageSegment) {
                append(if (ch == '_' || ch.isLetterOrDigit()) ch else '_')
            }
        }.ifBlank { "generated" }
        val startsAsIdentifier = normalized.first() == '_' || normalized.first().isLetter()
        val identifier = if (startsAsIdentifier) normalized else "_$normalized"
        return if (identifier in kotlinHardKeywords) "_$identifier" else identifier
    }

    /**
     * Kotlin 硬关键字集合，用于避免生成非法 package segment。
     */
    private val kotlinHardKeywords = setOf(
        "as",
        "break",
        "class",
        "continue",
        "do",
        "else",
        "false",
        "for",
        "fun",
        "if",
        "in",
        "interface",
        "is",
        "null",
        "object",
        "package",
        "return",
        "super",
        "this",
        "throw",
        "true",
        "try",
        "typealias",
        "typeof",
        "val",
        "var",
        "when",
        "while",
    )

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

    /**
     * LLT 中的 `pkg.cj` / `*.pkg.cj` 在同目录存在主测试 `.cj` 时是包 companion，
     * 只应作为同一编译单元的附加源参与主测试，不应生成独立入口测试。
     */
    private fun File.isGeneratedTestDataFile(): Boolean {
        if (!isFile || extension != "cj") return false
        if (!isPackageCompanionName()) return true
        val directory = parentFile ?: return true
        return directory.listFiles().orEmpty().none { sibling ->
            sibling.isFile && sibling.extension == "cj" && sibling != this && !sibling.isPackageCompanionName()
        }
    }

    /**
     * 判断文件名是否为 LLT 包 companion 文件名。
     */
    private fun File.isPackageCompanionName(): Boolean =
        name == "pkg.cj" || name.endsWith(".pkg.cj")
}




