package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 实际 PSI→symbol 查询投影声明返回属性；FOREIGN 不按局部 token 或外层 class 补授。 */
class AnalysisApiReturnedModifierTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/returnedModifiers",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun inheritedForeignFunctionsAndMacros(mainFile: CjFile) {
        val function = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java).single { it.name == "nativeRead" }
        val ordinary = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java).single { it.name == "ordinary" }
        val macro = PsiTreeUtil.findChildrenOfType(mainFile, CjMacroDeclaration::class.java).single()
        assertFalse(function.hasModifier(CjTokens.FOREIGN_KEYWORD))
        assertFalse(macro.hasModifier(CjTokens.FOREIGN_KEYWORD))
        analyzeForTest(mainFile) {
            val functionSymbol = function.symbol as CaNamedFunctionSymbol
            val macroSymbol = macro.symbol as CaMacroSymbol
            assertTrue(functionSymbol.isForeign)
            assertTrue(macroSymbol.isForeign)
            assertFalse((ordinary.symbol as CaNamedFunctionSymbol).isForeign)
            assertSame(function, functionSymbol.psi)
            assertSame(macro, macroSymbol.psi)
        }
    }

    @Test
    fun foreignClassDoesNotGrantForeignToMembers(mainFile: CjFile) {
        val property = PsiTreeUtil.findChildrenOfType(mainFile, CjProperty::class.java).single()
        val function = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java).single()
        analyzeForTest(mainFile) {
            assertFalse((property.symbol as CaPropertySymbol).isForeign)
            assertFalse((function.symbol as CaNamedFunctionSymbol).isForeign)
            assertSame(property, property.symbol.psi)
        }
    }

    @Test
    fun emptyFinalizerResultIsNotRecoveredFromSourceTokens(mainFile: CjFile) {
        val finalizer = PsiTreeUtil.findChildrenOfType(mainFile, CjFinalizer::class.java).single()
        assertTrue(finalizer.hasModifier(CjTokens.FOREIGN_KEYWORD))
        analyzeForTest(mainFile) {
            assertFalse((finalizer.symbol as CaFinalizerSymbol).isForeign)
            assertSame(finalizer, finalizer.symbol.psi)
        }
    }
}
