package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionTypeProvider

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `expressionTypeProvider.expectedExpressionType` 的抽象测试。
 *
 * 该测试定位目标表达式并断言上下文期望类型的公开渲染结果：
 * - 存在 `<expr>` 选择标记时定位被选中的表达式，用于完整调用/分支体等槽位；
 * - 否则沿用 caret 处的最内层表达式，保持既有夹具（含 callee 定位）语义不变。
 */
abstract class AbstractExpectedExpressionTypeTest : AbstractAnalysisApiComponentTest() {
    /**
     * 定位被测表达式：优先使用显式选择，其次回退到 caret。
     */
    private fun targetExpression(contextFile: CjFile, testServices: TestServices): CjExpression {
        val markerProvider = testServices.expressionMarkerProvider
        return if (markerProvider.getSelectionOrNull(contextFile) != null) {
            markerProvider.getTopmostSelectedElementOfType<CjExpression>(contextFile)
        } else {
            markerProvider.getBottommostElementOfTypeAtCaret<CjExpression>(contextFile)
        }
    }
    /**
     * 执行 expected expression type 查询测试。
     *
     * 方法定位目标表达式并断言上下文期望类型的公开渲染结果。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val actual = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val expression = targetExpression(contextFile, testServices)
            val expectedType = expression.expectedType

            buildString {
                appendLine("expression: ${expression.text}")
                appendLine(
                    "expectedType: ${
                        expectedType
                            ?.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)
                            ?.let(::normalizeTypeRendering)
                    }",
                )
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
