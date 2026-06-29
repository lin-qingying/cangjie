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
 *
 * 该测试验证期望类型为函数类型时公开 `CaFunctionType` 标志位是否稳定。
 */
abstract class AbstractFunctionClassKindTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行函数类型种类信息测试。
     *
     * 方法读取选中表达式的 expected type，并输出 C 函数、闭包和变长参数等公开标志。
     */
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
