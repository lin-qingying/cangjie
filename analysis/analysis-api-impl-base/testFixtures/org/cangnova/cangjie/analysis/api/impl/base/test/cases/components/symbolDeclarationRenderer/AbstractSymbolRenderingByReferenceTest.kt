package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForDebug
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.test.services.assertions
import org.cangnova.cangjie.test.services.TestServices

/**
 * symbol declaration renderer by reference 抽象测试。
 *
 * 对齐 Kotlin `AbstractSymbolRenderingByReferenceTest`：
 * 通过 `<caret>` 定位引用表达式，按 Analysis API 引用解析出 declaration symbol，
 * 再使用 debug renderer 与同名 `.txt` golden 对比。
 */
abstract class AbstractSymbolRenderingByReferenceTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val referenceExpression = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjReferenceExpression>(mainFile)

        val renderedString = analyzeForTest(referenceExpression) {
            val symbol = referenceExpression.resolveToSymbol()
            testServices.assertions.assertNotNull(symbol) {
                "引用应能恢复成 declaration symbol"
            }
            testServices.assertions.assertTrue(symbol is CaDeclarationSymbol) {
                "symbol rendering by reference 只接受 declaration symbol"
            }
            (symbol as CaDeclarationSymbol).render(CaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES)
        }

        testServices.assertions.assertEqualsToTestOutputFile(renderedString)
    }
}
