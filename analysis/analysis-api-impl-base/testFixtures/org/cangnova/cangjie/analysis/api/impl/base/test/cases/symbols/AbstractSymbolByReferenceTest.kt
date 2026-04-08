package org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceTestUtils.findUsageSimpleName
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceSymbolTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedOriginalPsiClass
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedSymbolClass
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedSymbolName
import org.cangnova.cangjie.analysis.api.impl.base.test.referenceTargetName
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * reference -> symbol 抽象测试。
 *
 * 该测试锁定的是“通过引用入口恢复公开 symbol”的契约，而不是再从 declaration 反推。
 */
abstract class AbstractSymbolByReferenceTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiReferenceSymbolTestDirectives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val reference = findUsageSimpleName(mainFile, directives.referenceTargetName)

        analyzeForTest(reference) {
            val symbol = reference.resolveToSymbol()
            assertNotNull(symbol, "引用应能恢复成公开 symbol")
            assertEquals(directives.expectedSymbolClass, symbol!!::class.simpleName)
            assertEquals(directives.expectedSymbolName, symbol.name?.asString())

            val originalPsi = symbol.getOriginalPsi()
            assertNotNull(originalPsi, "symbol 应能回到 original PSI")
            assertEquals(directives.expectedOriginalPsiClass, originalPsi!!::class.simpleName)
        }
    }
}
