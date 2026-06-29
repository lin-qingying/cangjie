package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * 新增 PSI 入口的 public symbol provider 回归测试。
 *
 * 目标是锁定 `PSI -> symbol -> original PSI` 这条公开契约，
 * 防止匿名函数、accessor、extend、enum constructor、field 等入口再次退化成
 * “只能通过 owner 间接恢复 symbol” 的状态。
 */
class AnalysisApiSymbolProviderEntryTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/symbolProviderEntries",
) {
    /**
     * 使用 standalone CFIR 配置验证 PSI 入口到 public symbol 的恢复路径。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证各类新增 PSI 入口能直接创建 symbol，并且 symbol 能恢复到同一个 PSI 实例。
     */
    @Test
    fun psiEntrySymbols(mainFile: CjFile) {
        val macroDeclaration = PsiTreeUtil.findChildrenOfType(mainFile, CjMacroDeclaration::class.java).single()
        val extendDeclaration = PsiTreeUtil.findChildrenOfType(mainFile, CjExtend::class.java).single()
        val fieldDeclaration = PsiTreeUtil.findChildrenOfType(mainFile, CjFieldVariable::class.java).single()
        val constructorDeclaration = PsiTreeUtil.findChildrenOfType(mainFile, CjSecondaryConstructor::class.java).single()
        val finalizerDeclaration = PsiTreeUtil.findChildrenOfType(mainFile, CjFinalizer::class.java).single()
        val enumConstructorDeclaration = PsiTreeUtil.findChildrenOfType(mainFile, CjEnumConstructor::class.java)
            .single { enumConstructor -> enumConstructor.name == "Ready" }
        val getterDeclaration = PsiTreeUtil.findChildrenOfType(mainFile, CjPropertyAccessor::class.java).single { accessor ->
            accessor.isGetter
        }
        val setterDeclaration = PsiTreeUtil.findChildrenOfType(mainFile, CjPropertyAccessor::class.java).single { accessor ->
            accessor.isSetter
        }
        val functionLiteral = PsiTreeUtil.findChildrenOfType(mainFile, CjFunctionLiteral::class.java).single()
        val patternVariable = PsiTreeUtil.findChildrenOfType(mainFile, CjPatternVariable::class.java).single { variable ->
            PsiTreeUtil.findChildrenOfType(variable, CjBindingPattern::class.java).any { binding ->
                binding.name == "left"
            }
        }
        val patternBinding = PsiTreeUtil.findChildrenOfType(mainFile, CjBindingPattern::class.java)
            .single { binding -> binding.name == "left" }

        analyzeForTest(mainFile) {
            fun assertRestoresToSamePsi(psi: PsiElement, symbol: CaSymbol) {
                val originalPsi = symbol.psi
                assertNotNull(originalPsi, "Symbol `${symbol::class.simpleName}` should keep original PSI.")
                assertSame(psi, originalPsi)
            }

            val macroSymbol = macroDeclaration.symbol as CaMacroSymbol
            val extendSymbol = extendDeclaration.symbol as CaExtendSymbol
            val fieldSymbol = fieldDeclaration.symbol as CaFieldSymbol
            val constructorSymbol = constructorDeclaration.symbol as CaConstructorSymbol
            val finalizerSymbol = finalizerDeclaration.symbol as CaFinalizerSymbol
            val enumConstructorSymbol = enumConstructorDeclaration.symbol as CaEnumConstructorSymbol
            val getterSymbol = getterDeclaration.symbol as CaPropertyGetterSymbol
            val setterSymbol = setterDeclaration.symbol as CaPropertySetterSymbol
            val anonymousFunctionSymbol = functionLiteral.symbol as CaAnonymousFunctionSymbol
            val patternVariableSymbol = patternVariable.symbol as CaPatternVariableSymbol
            val patternBindingSymbol = patternBinding.symbol as CaPatternBindingSymbol

            assertRestoresToSamePsi(macroDeclaration, macroSymbol)
            assertRestoresToSamePsi(extendDeclaration, extendSymbol)
            assertRestoresToSamePsi(fieldDeclaration, fieldSymbol)
            assertRestoresToSamePsi(constructorDeclaration, constructorSymbol)
            assertRestoresToSamePsi(finalizerDeclaration, finalizerSymbol)
            assertRestoresToSamePsi(enumConstructorDeclaration, enumConstructorSymbol)
            assertRestoresToSamePsi(getterDeclaration, getterSymbol)
            assertRestoresToSamePsi(setterDeclaration, setterSymbol)
            assertRestoresToSamePsi(functionLiteral, anonymousFunctionSymbol)
            assertRestoresToSamePsi(patternVariable, patternVariableSymbol)
            assertRestoresToSamePsi(patternBinding, patternBindingSymbol)

            assertEquals(CaSymbolLocation.TOP_LEVEL, extendSymbol.location)
            assertEquals(CaSymbolLocation.PROPERTY, getterSymbol.location)
            assertEquals(CaSymbolLocation.PROPERTY, setterSymbol.location)
            assertEquals(CaSymbolLocation.LOCAL, anonymousFunctionSymbol.location)
            assertEquals(CaSymbolLocation.LOCAL, patternVariableSymbol.location)
            assertEquals(CaSymbolLocation.LOCAL, patternBindingSymbol.location)

            assertEquals("state", getterSymbol.owningProperty.name.asString())
            assertEquals("state", setterSymbol.owningProperty.name.asString())
            assertEquals("value", setterSymbol.parameter.name.asString())
            assertEquals(
                "prettyPrint",
                extendSymbol.declaredMemberScope.callables(Name.identifier("prettyPrint")).single().name?.asString(),
            )
        }
    }
}
