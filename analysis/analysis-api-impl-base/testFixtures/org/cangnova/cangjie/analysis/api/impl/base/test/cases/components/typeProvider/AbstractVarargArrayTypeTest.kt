package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeProvider

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `typeProvider.varargArrayType` 的抽象测试。
 */
abstract class AbstractVarargArrayTypeTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val actual = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val declarationAtCaret =
                testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<CjParameter>(contextFile)
                    ?: error("No parameter found under the caret")
            val symbol = declarationAtCaret.symbol

            assert(symbol is CaValueParameterSymbol) {
                "Expected value parameter, got ${symbol::class} instead"
            }

            val varargArrayType = (symbol as CaValueParameterSymbol).varargArrayType
            if (varargArrayType != null) {
                varargArrayType.render(CaTypeRendererForSource.WITH_SHORT_NAMES).let(::normalizeTypeRendering)
            } else {
                "NOT_VARARG"
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
