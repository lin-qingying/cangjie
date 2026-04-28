package org.cangnova.cangjie.analysis.api.impl.base.test

import java.io.File

/**
 * Analysis API generated tests 一致性校验器。
 *
 * 它负责校验三件事：
 * 1. 注册表展开后的全部 runner 文件都已存在；
 * 2. `tests-gen` 目录中的文件内容与生成器当前产物完全一致；
 * 3. `tests-gen` 中不存在注册表之外的多余 runner。
 *
 * 这样 `analysis-api-cfir` 的 generated tests 不再只是“能生成”，
 * 而是具备与注册表、testData 同步演进的框架级校验入口。
 */
object GeneratedAnalysisApiTestConsistencyChecker {
    @JvmStatic
    fun main(args: Array<String>) {
        val projectRoot = if (args.isNotEmpty()) File(args[0]) else File(System.getProperty("user.dir"))
        val expectedFiles = TestGeneratorForAnalysisApi.generatedSuites(projectRoot)
        val expectedByPath = expectedFiles.associateBy { it.outputFile.canonicalFile.invariantSeparatorsPath }

        expectedFiles.forEach { generatedFile ->
            val actualFile = generatedFile.outputFile
            check(actualFile.isFile) {
                buildString {
                    appendLine("缺少 generated Analysis API runner：${actualFile.invariantSeparatorsPath}")
                    appendLine("请先运行 :analysis:analysis-api-cfir:generateTests 更新 tests-gen。")
                }
            }

            val actualContent = actualFile.readText(Charsets.UTF_8)
            check(actualContent == generatedFile.content) {
                buildString {
                    appendLine("generated Analysis API runner 已过期：${actualFile.invariantSeparatorsPath}")
                    appendLine("请运行 :analysis:analysis-api-cfir:generateTests 更新 tests-gen。")
                }
            }
        }

        val testsGenRoot = projectRoot.resolve(TestGeneratorForAnalysisApi.generatedOutputRoot)
        val actualGeneratedFiles = testsGenRoot.walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .map { file -> file.canonicalFile.invariantSeparatorsPath }
            .toSet()

        val expectedGeneratedFiles = expectedByPath.keys
        val unexpectedFiles = actualGeneratedFiles - expectedGeneratedFiles
        check(unexpectedFiles.isEmpty()) {
            buildString {
                appendLine("tests-gen 中存在注册表之外的 generated runner：")
                unexpectedFiles.sorted().forEach { path -> appendLine(path) }
                appendLine("请清理多余文件，或修正 Analysis API generated test DSL。")
            }
        }
    }
}
