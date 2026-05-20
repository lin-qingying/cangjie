package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.dataFlowInfoProvider

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `dataFlowInfoProvider.smartCastInfo` 的抽象测试。
 */
abstract class AbstractSmartCastInfoTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val expression = testServices.expressionMarkerProvider
            .getTopmostSelectedElementOfType<CjExpression>(mainFile)

        val actual = analyzeForTest(expression) {
            val info = expression.getDataFlowInfo()
            buildString {
                appendLine("expression: ${expression.text}")
                appendLine("smartCastInfo:")
                appendLine("    expressionType: ${info.expressionType?.render(CaTypeRendererForSource.WITH_SHORT_NAMES)?.let(::normalizeTypeRendering)}")
                appendLine("    stability: ${info.stability}")
                appendLine("    isPureReference: ${info.isPureReference}")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
