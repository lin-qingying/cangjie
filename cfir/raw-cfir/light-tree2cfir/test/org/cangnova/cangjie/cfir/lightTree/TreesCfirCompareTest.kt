package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File

/**
 * PSI vs LightTree CFIR 对比测试（对齐 K2 的 TreesCompareTest）。
 *
 * 遍历 psi2cfir/testData/rawBuilder 目录下所有 .cj 测试数据文件，
 * 同时通过 PSI 和 LightTree 两条路径构建 CFIR，
 * 验证两种路径的渲染结果一致。
 *
 * 如果所有文件都通过对比，说明 LightTree2Cfir 与 PsiRawCfirBuilder 功能等价。
 */
@RunWith(JUnit3RunnerWithInners::class)
class TreesCfirCompareTest : AbstractLightTree2CfirConverterTestCase() {

    /**
     * 对比 rawBuilder 目录下所有测试数据文件。
     */
    fun testCompareAllRawBuilderTestData() {
        val testDataRoot = resolveTestDataPath(RAW_BUILDER_TEST_DATA_PATH)
        if (!testDataRoot.isDirectory) {
            println("WARNING: testData directory not found: ${testDataRoot.path}")
            return
        }

        var totalFiles = 0
        var passedFiles = 0
        val failedFiles = mutableListOf<Pair<File, String>>()

        testDataRoot.walkTopDown()
            .filter { it.isFile && it.extension == "cj" }
            .sorted()
            .forEach { file ->
                totalFiles++
                try {
                    val sourceText = loadFile(file.path).trim()
                    doCompareTest(sourceText, file.nameWithoutExtension)
                    passedFiles++
                } catch (e: AssertionError) {
                    failedFiles.add(file to e.message.orEmpty())
                } catch (e: Exception) {
                    failedFiles.add(file to "Exception: ${e.message}")
                }
            }

        println("TreesCfirCompareTest: $passedFiles/$totalFiles files passed")

        if (failedFiles.isNotEmpty()) {
            val report = buildString {
                appendLine("${failedFiles.size} file(s) differ between PSI and LightTree:")
                failedFiles.forEach { (file, msg) ->
                    appendLine("  - ${file.relativeTo(testDataRoot).invariantSeparatorsPath}")
                    // 截断过长的错误信息
                    val shortMsg = msg.lines().take(5).joinToString("\n    ")
                    appendLine("    $shortMsg")
                }
            }
            throw AssertionError(report)
        }
    }

    companion object {
        private const val RAW_BUILDER_TEST_DATA_PATH =
            "cfir/raw-cfir/psi2cfir/testData/rawBuilder"
    }
}
