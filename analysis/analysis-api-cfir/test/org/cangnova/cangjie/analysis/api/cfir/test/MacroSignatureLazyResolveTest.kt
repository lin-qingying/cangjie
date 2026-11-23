package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 冷查询宏签名，类型来自实际 std.ast 库；不预热宏返回类型或正文 diagnostics。 */
class MacroSignatureLazyResolveTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/macroSignatureLazy",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** 参数类型使用实际 std.ast CJO，与既有 standalone 库测试使用同一服务宿主。 */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(CaAnalysisApiDecompiledTestServiceRegistrar)

    @Test
    fun directParameterType(mainFile: CjFile) = assertColdSignature(mainFile, parameterFirst = true, parameterCount = 1)

    @Test
    fun attributedParameterTypes(mainFile: CjFile) = assertColdSignature(mainFile, parameterFirst = true, parameterCount = 2)

    @Test
    fun defaultReturnType(mainFile: CjFile) = assertColdSignature(mainFile, parameterFirst = false, parameterCount = 1)

    private fun assertColdSignature(mainFile: CjFile, parameterFirst: Boolean, parameterCount: Int) {
        val psi = PsiTreeUtil.findChildrenOfType(mainFile, CjMacroDeclaration::class.java).single()
        assertEquals(parameterCount, psi.valueParameters.size)
        analyzeForTest(psi) {
            val facade = (this as CaCfirSession).resolutionFacade
            val raw = (psi.resolveToCfirSymbol(facade) as CfirMacroDeclarationSymbol).cfir
            assertEquals(CfirResolvePhase.RAW_CFIR, raw.resolvePhase, "首查前仅恢复 raw owner")
            val macroSymbol = assertInstanceOf(CaMacroSymbol::class.java, psi.symbol)
            if (parameterFirst) {
                // 直接从参数 PSI 首查 returnType，不能依赖先读取 macroSymbol.returnType。
                assertTokens(psi.valueParameters.first().symbol.returnType)
            } else {
                assertTokens(macroSymbol.returnType)
            }
            val parameters = macroSymbol.valueParameters
            assertEquals(parameterCount, parameters.size)
            parameters.forEachIndexed { index, parameter ->
                assertTokens(parameter.returnType)
                assertSame(psi.valueParameters[index], parameter.psi)
                assertSame(raw.symbol, raw.valueParameters[index].containingDeclarationSymbol)
                assertTrue(raw.valueParameters[index].returnTypeRef is CfirResolvedTypeRef)
            }
            assertTokens(macroSymbol.returnType)
            assertSame(psi, macroSymbol.psi)
            assertSignatureOnly(raw)
        }
    }

    private fun assertTokens(type: CaType) {
        val classType = assertInstanceOf(CaUsualClassType::class.java, type)
        // ClassId 只作为类型结果 oracle，不用它查询或恢复声明。
        assertEquals("std.ast.Tokens", classType.classId.asSingleFqName().asString())
    }

    private fun assertSignatureOnly(macro: CfirMacroDeclaration) {
        assertTrue(macro.resolvePhase >= CfirResolvePhase.TYPES)
        assertTrue(macro.resolvePhase <= CfirResolvePhase.IMPLICIT_TYPES, "类型查询不得强制 BODY_RESOLVE")
    }
}
