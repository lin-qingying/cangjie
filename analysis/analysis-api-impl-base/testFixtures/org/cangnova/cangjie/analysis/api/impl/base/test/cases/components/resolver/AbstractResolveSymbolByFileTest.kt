package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver

import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceTestUtils.isUsageSimpleNameForAnalysisApiTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `allByPsi` 文件级符号解析测试。
 *
 * 只遍历当前 resolver 公开 API 的稳定入口 `CjReferenceExpression.resolveToSymbol()`。
 */
abstract class AbstractResolveSymbolByFileTest : AbstractResolveSymbolTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val referenceExpressions = mainFile.collectDescendantsOfType<CjSimpleNameExpression>()
            .filter { it.isUsageSimpleNameForAnalysisApiTest() }

        val actual = buildString {
            referenceExpressions.forEachIndexed { index, referenceExpression ->
                appendLine("CjSimpleNameExpression: ${referenceExpression.text}")
                append(
                    analyzeForTest(referenceExpression) {
                        renderSymbolForResolveTest(referenceExpression.resolveToSymbol())
                    }.prependIndent("  "),
                )

                if (index != referenceExpressions.lastIndex) {
                    appendLine()
                    appendLine()
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = "symbol.txt")
    }
}
