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
 * 该测试从 caret 处表达式读取上下文期望类型，并用 qualified renderer 固定公开输出。
 */
abstract class AbstractExpectedExpressionTypeTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行 expected expression type 查询测试。
     *
     * 方法定位目标表达式并断言上下文期望类型的公开渲染结果。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val actual = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val expression = testServices.expressionMarkerProvider
                .getBottommostElementOfTypeAtCaret<CjExpression>(contextFile)
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
