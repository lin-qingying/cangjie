package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeInfoProvider

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `typeInfoProvider.functionClassKind` 的抽象测试。
 */
abstract class AbstractFunctionClassKindTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val expression = testServices.expressionMarkerProvider
            .getTopmostSelectedElementOfType<CjExpression>(mainFile)

        val actual = copyAwareAnalyzeForTest(expression) { contextExpression ->
            val type = contextExpression.expectedType
            val functionType = type as? CaFunctionType
            buildString {
                appendLine("expression: ${contextExpression.text}")
                appendLine("type: ${type?.render(CaTypeRendererForSource.WITH_SHORT_NAMES)?.let(::normalizeTypeRendering)}")
                appendLine("isFunctionType: ${functionType != null}")
                if (functionType != null) {
                    appendLine("isCFunction: ${functionType.isCFunction}")
                    appendLine("isClosureType: ${functionType.isClosureType}")
                    appendLine("hasVariableLengthArgument: ${functionType.hasVariableLengthArgument}")
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
