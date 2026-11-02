package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.symbols.CaModifierExplicitness
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.symbol
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.cfir.symbols.asPublicModalityExplicitness
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunctionCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildPropertyCopy
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.CfirModifierExplicitness
import org.cangnova.cangjie.cfir.declarations.resolvedWithEffectiveModality
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeParameter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证公开 Analysis API 的显式性枚举来自 CFIR 三态值，而旧 Boolean 只表示 EXPLICIT。 */
class AnalysisApiModifierExplicitnessTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/modifierExplicitness",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun sourceMemberModalityRequiresStatusResolve(mainFile: CjFile) {
        val functions = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java).associateBy { it.name }
        val property = PsiTreeUtil.findChildrenOfType(mainFile, CjProperty::class.java).single()
        analyzeForTest(mainFile) {
            val required = requireNotNull(functions["required"]).symbol as CaNamedFunctionSymbol
            val provided = requireNotNull(functions["provided"]).symbol as CaNamedFunctionSymbol
            val pending = requireNotNull(functions["pending"]).symbol as CaNamedFunctionSymbol
            assertEquals(CaSymbolModality.ABSTRACT, required.modality)
            assertEquals(CaSymbolModality.OPEN, provided.modality)
            assertEquals(CaSymbolModality.ABSTRACT, pending.modality)
            assertEquals(CaSymbolModality.OPEN, (property.symbol as CaPropertySymbol).modality)

            // 库状态与 backing PSI 的 public/open 冲突时，公开符号必须服从库状态。
            val sourceFunction = requireNotNull(functions["sourceMarked"]).symbol as CaCfirNamedFunctionSymbol
            val sourceProperty = property.symbol as CaCfirPropertySymbol
            val originalFunction = sourceFunction.cfirSymbol.cfir
            val originalProperty = sourceProperty.cfirSymbol.cfir
            val libraryStatus = CfirDeclarationStatusImpl(Visibilities.Unknown, Modality.FINAL).apply {
                visibilityExplicitness = CfirModifierExplicitness.UNKNOWN
                modalityExplicitness = CfirModifierExplicitness.UNKNOWN
            }.resolvedWithEffectiveModality()
            val libraryFunction = buildNamedFunctionCopy(originalFunction) {
                origin = CfirDeclarationOrigin.Library
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                symbol = CfirNamedFunctionSymbol(originalFunction.symbol.callableId)
                status = libraryStatus
            }
            val libraryProperty = buildPropertyCopy(originalProperty) {
                origin = CfirDeclarationOrigin.Library
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                symbol = CfirPropertySymbol(originalProperty.symbol.callableId)
                status = libraryStatus
            }
            val cfirSession = this as CaCfirSession
            val functionSymbol = CaCfirNamedFunctionSymbol(libraryFunction.symbol, cfirSession)
            val propertySymbol = CaCfirPropertySymbol(libraryProperty.symbol, cfirSession)
            assertSame(functions["sourceMarked"], functionSymbol.psi)
            assertSame(property, propertySymbol.psi)
            assertEquals(CaSymbolVisibility.UNKNOWN, functionSymbol.visibility)
            assertEquals(CaSymbolVisibility.UNKNOWN, propertySymbol.visibility)
            assertEquals(CaSymbolModality.FINAL, functionSymbol.modality)
            assertEquals(CaSymbolModality.FINAL, propertySymbol.modality)
            assertEquals(CaModifierExplicitness.UNKNOWN, functionSymbol.modalityExplicitness)
            assertEquals(CaModifierExplicitness.UNKNOWN, propertySymbol.modalityExplicitness)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun illegalSourceModifiersKeepExplicitnessFacts(mainFile: CjFile) {
        val function = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java).single()
        val property = PsiTreeUtil.findChildrenOfType(mainFile, CjProperty::class.java).single()
        analyzeForTest(mainFile) {
            val functionSymbol = function.symbol as CaNamedFunctionSymbol
            val propertySymbol = property.symbol as CaPropertySymbol
            for (symbol in listOf(functionSymbol, propertySymbol)) {
                assertEquals(CaSymbolModality.FINAL, symbol.modality)
                assertEquals(CaModifierExplicitness.EXPLICIT, symbol.modalityExplicitness)
                assertTrue(symbol.isModalityExplicit)
            }
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun sourceModifiersExposeExplicitImplicitAndUnknownStates(mainFile: CjFile) {
        val functions = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java).associateBy { it.name }
        val typeParameter = PsiTreeUtil.findChildrenOfType(mainFile, CjTypeParameter::class.java).single()
        val statusWithoutModality = CfirDeclarationStatusImpl()
        statusWithoutModality.modalityExplicitness = CfirModifierExplicitness.EXPLICIT
        assertEquals(CfirModifierExplicitness.UNKNOWN, statusWithoutModality.modalityExplicitness)
        assertFalse(statusWithoutModality.isModalityExplicit)
        assertEquals(
            CaModifierExplicitness.UNKNOWN,
            statusWithoutModality.asPublicModalityExplicitness(),
        )

        analyzeForTest(mainFile) {
            val explicit = requireNotNull(functions["explicit"]).symbol as CaNamedFunctionSymbol
            assertEquals(CaModifierExplicitness.EXPLICIT, explicit.modalityExplicitness)
            assertTrue(explicit.isModalityExplicit)
            assertEquals(CaModifierExplicitness.EXPLICIT, explicit.visibilityExplicitness)
            assertTrue(explicit.isVisibilityExplicit)

            val implicit = requireNotNull(functions["implicit"]).symbol as CaNamedFunctionSymbol
            assertEquals(CaModifierExplicitness.IMPLICIT, implicit.modalityExplicitness)
            assertFalse(implicit.isModalityExplicit)
            assertEquals(CaModifierExplicitness.IMPLICIT, implicit.visibilityExplicitness)
            assertFalse(implicit.isVisibilityExplicit)

            val implicitlyFinalTypeParameter = typeParameter.symbol as CaTypeParameterSymbol
            assertEquals(CaSymbolModality.FINAL, implicitlyFinalTypeParameter.modality)
            assertEquals(CaModifierExplicitness.UNKNOWN, implicitlyFinalTypeParameter.modalityExplicitness)
            assertFalse(implicitlyFinalTypeParameter.isModalityExplicit)
        }
    }
}
