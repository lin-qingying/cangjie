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
 */
abstract class AbstractExpectedExpressionTypeTest : AbstractAnalysisApiComponentTest() {
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
