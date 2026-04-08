package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceTestUtils.findUsageSimpleName
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceSymbolTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedRenderedDeclaration
import org.cangnova.cangjie.analysis.api.impl.base.test.referenceTargetName
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * symbol declaration renderer by reference 抽象测试。
 *
 * 这里不从 declaration 直接取 symbol，而是从引用入口恢复 symbol 后再渲染，
 * 锁定“引用链与 renderer 链的一致性”。
 */
abstract class AbstractSymbolRenderingByReferenceTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiReferenceSymbolTestDirectives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val reference = findUsageSimpleName(mainFile, directives.referenceTargetName)

        analyzeForTest(reference) {
            val symbol = reference.resolveToSymbol()
            assertNotNull(symbol, "引用应能恢复成 declaration symbol")
            assertTrue(symbol is CaDeclarationSymbol, "symbol rendering by reference 只接受 declaration symbol")
            assertEquals(
                directives.expectedRenderedDeclaration,
                (symbol as CaDeclarationSymbol).render(CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES),
            )
        }
    }
}
