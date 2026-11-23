package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals as checkEquals
import kotlin.test.assertIs as checkIs
import kotlin.test.assertSame as checkSame

/** 两套 raw builder 按官方 ParseMacroBody 保留专用宏与 Tokens 签名，不解析任何正文。 */
@RunWith(JUnit3RunnerWithInners::class)
class MacroRawSignatureTest : AbstractLightTree2CfirConverterTestCase() {
    fun testDefaultTokensReturnKeepsOrdinaryFunctionImplicit() {
        forEachBuilder("""
            macro package sample.macros.raw
            import std.ast.*
            public macro Identity(input: Tokens) { return input }
            func helper(input: Tokens) { return input }
        """.trimIndent()) { file ->
            val macro = file.declarations.filterIsInstance<CfirMacroDeclaration>().single()
            checkEquals(CfirResolvePhase.RAW_CFIR, macro.resolvePhase)
            checkEquals("Tokens", checkIs<CfirUserTypeRef>(macro.returnTypeRef).qualifier.single().name.asString())
            checkEquals("Tokens", checkIs<CfirUserTypeRef>(macro.valueParameters.single().returnTypeRef).qualifier.single().name.asString())
            checkSame(macro.symbol, macro.valueParameters.single().containingDeclarationSymbol)
            checkIs<CfirImplicitTypeRef>(file.declarations.filterIsInstance<CfirNamedFunction>().single().returnTypeRef)
        }
    }

    fun testExplicitMacroContextIsPreservedForOfficialInvalidParameterDiagnostic() {
        // 官方 ParseMacroParameterList/CheckMacroParamType 拒绝此参数；raw 层不得偷偷改为 Tokens。
        forEachBuilder("""
            macro package sample.macros.raw.invalid
            import std.ast.*
            public macro Invalid(input: MacroContext): Tokens { return input }
        """.trimIndent()) { file ->
            val macro = file.declarations.filterIsInstance<CfirMacroDeclaration>().single()
            checkEquals("MacroContext", checkIs<CfirUserTypeRef>(macro.valueParameters.single().returnTypeRef).qualifier.single().name.asString())
            checkSame(macro.symbol, macro.valueParameters.single().containingDeclarationSymbol)
        }
    }

    private fun forEachBuilder(source: String, assertion: (CfirFile) -> Unit) {
        val psiSession = createTestSession()
        assertion(PsiRawCfirBuilder(psiSession, BodyBuildingMode.LAZY_BODIES)
            .buildCfirFile(createCjFile("macroSignaturePsi", source)))
        assertion(buildCfirFileFromLightTree(source, bodyBuildingMode = BodyBuildingMode.LAZY_BODIES))
    }
}
