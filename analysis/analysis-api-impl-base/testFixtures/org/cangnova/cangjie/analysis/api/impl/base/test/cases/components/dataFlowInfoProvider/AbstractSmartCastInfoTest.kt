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
 *
 * 该测试从选中表达式读取 data-flow 信息，并聚焦 smart-cast 相关的表达式类型、稳定性和纯引用状态。
 */
abstract class AbstractSmartCastInfoTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行 smart-cast 信息测试。
     *
     * 方法在目标表达式上查询 data-flow smart-cast 结果，并将公开类型信息写入 golden。
     */
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
