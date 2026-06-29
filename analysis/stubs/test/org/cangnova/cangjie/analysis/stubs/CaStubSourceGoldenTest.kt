package org.cangnova.cangjie.analysis.stubs

import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions
import org.junit.jupiter.api.Test

/**
 * 验证源码文件经过 analysis:stubs 摘要构建后的 golden 输出。
 */
class CaStubSourceGoldenTest : AbstractAnalysisApiExecutionTest(
    "analysis/stubs/testData/source",
) {
    /**
     * 使用 standalone CFIR 分析 API 配置执行 source stub golden 测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证主测试文件的顶层声明和成员摘要与 golden 文件一致。
     */
    @Test
    fun topLevelDeclarations(mainFile: CjFile, testServices: TestServices) {
        val summary = CaStubSummaryBuilder().build(mainFile)
        testServices.assertions.assertEqualsToTestOutputFile(
            actual = renderSummary(summary),
            extension = ".stubs.txt",
        )
    }

    /**
     * 将单文件 stub 摘要渲染为稳定的 golden 文本。
     */
    private fun renderSummary(summary: CaStubFileSummary): String {
        return buildString {
            appendLine("fileKey=${summary.fileKey.substringAfterLast('/').substringAfterLast('\\')}")
            appendLine("kind=${summary.stubKind ?: "<missing>"}")
            appendLine("package=${summary.packageFqName?.asString() ?: "<missing>"}")
            appendLine("topLevelClassifiers=${summary.topLevelClassifierNames.map { it.asString() }.sorted()}")
            appendLine("topLevelCallables=${summary.topLevelCallableNames.map { it.asString() }.sorted()}")
            appendLine("classMembers=")
            if (summary.classMemberNames.isEmpty()) {
                append("  <none>")
            } else {
                summary.classMemberNames.toSortedMap(compareBy { it.asString() }).forEach { (classId, names) ->
                    appendLine("  ${classId.asFqNameString()}=${names.map { it.asString() }.sorted()}")
                }
            }
        }.trimEnd()
    }

    /**
     * 将 Windows 换行统一为测试 golden 使用的 LF。
     */
    private fun String.normalizeLineSeparators(): String = replace("\r\n", "\n")
}
