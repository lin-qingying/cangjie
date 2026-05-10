package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Analysis API 表面入口测试。
 *
 * 对齐 Kotlin `AnalysisApiSurfaceTest` 的框架形态：
 * 使用真实 testData 文件、统一 configurator 和 Analysis API session，
 * 不在 CFIR 行为测试里手写 token/mock 契约。
 */
class AnalysisApiSurfaceTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/surface",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun sourceReferenceResolution(mainFile: CjFile, testServices: TestServices) {
        val referenceExpression = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjReferenceExpression>(mainFile)

        analyzeForTest(referenceExpression) {
            val symbol = referenceExpression.resolveToSymbol()
            assertNotNull(symbol, "surface reference should resolve through Analysis API")
            val functionSymbol = assertInstanceOf(CaFunctionSymbol::class.java, symbol)
            assertEquals("target", functionSymbol.callableId?.callableName?.asString())
        }
    }
}
