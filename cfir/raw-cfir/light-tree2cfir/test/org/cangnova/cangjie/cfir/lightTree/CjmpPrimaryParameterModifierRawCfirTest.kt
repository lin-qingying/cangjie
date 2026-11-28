package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.memberParameterFieldInfo
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentPayload
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroParameterSignature
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawConstructedClassScope
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawOwnerSnapshot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawTypeParameterScope
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.CjMacroFragmentContext
import org.cangnova.cangjie.parsing.CjMemberParsingContext
import org.cangnova.cangjie.parsing.CjParameterGrammar
import org.cangnova.cangjie.parsing.CjmpFileRole
import org.cangnova.cangjie.parsing.ParsingFileFacts
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.parsing.createMacroFragmentParserSeed
import org.cangnova.cangjie.psi.CjMacroCodeFragment
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

/** standalone primary ParameterSlice 依真实签名消费返回属性，不借不存在的 PSI ownerFunction。 */
@RunWith(JUnit3RunnerWithInners::class)
class CjmpPrimaryParameterModifierRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiRejectedPrimarySliceUsesEmptyReturnedAttributes() = check(lightTree = false, common = true)
    fun testLightTreeRejectedPrimarySliceUsesEmptyReturnedAttributes() = check(lightTree = true, common = true)
    fun testPsiAcceptedPrimarySliceKeepsReturnedAttributes() = check(lightTree = false, common = false)
    fun testLightTreeAcceptedPrimarySliceKeepsReturnedAttributes() = check(lightTree = true, common = false)

    private fun check(lightTree: Boolean, common: Boolean) {
        for (grammar in listOf(CjParameterGrammar.CLASS_PRIMARY, CjParameterGrammar.STRUCT_PRIMARY)) {
            val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
            val session = createTestSession(settings)
            val cjmp = CfirCjmpSettingsComponent(explicitMode = if (common) CfirCjmpMode.COMMON else CfirCjmpMode.SPECIFIC)
            session.register(CfirCjmpSettingsComponent::class, cjmp)
            val parsingContext = cjmp.toParsingContext(settings)
            val kind = if (grammar == CjParameterGrammar.CLASS_PRIMARY) "class" else "struct"
            val host = createCjFile("primaryOwner", "$kind Owner { Owner(public var initial: Int64) {} }")
            host.cjmpParsingContext = parsingContext
            val raw = PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder()).buildPreMacroCfirFile(host)
            val owner = assertIs<CfirClassLikeDeclaration>(raw.cfirFile.declarations.single())
            val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single { it.isPrimary }
            val signature = MacroParameterSignature.Declaration(constructor.symbol, grammar)
            val ownerParameters = owner.typeParameters.map { it.symbol }
            val snapshot = MacroRawOwnerSnapshot(FqName.ROOT, listOf(owner.symbol),
                assertIs<ConeSimpleCangJieType>(constructor.dispatchReceiverType), false, null, signature,
                CjMemberParsingContext(if (kind == "class") CjMemberParsingContext.Kind.CLASS else CjMemberParsingContext.Kind.STRUCT, "Owner"),
                listOf(MacroRawTypeParameterScope(false, ownerParameters)),
                listOf(MacroRawConstructedClassScope(owner.symbol, ownerParameters)))
            val seed = createMacroFragmentParserSeed(ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE),
                parsingContext, CjMacroFragmentContext.ParameterSlice(grammar, "Owner"))
            val source = "public specific var generated: Int64"
            val metadata = CfirAnnotationMetadataBuilder()
            val payload = if (lightTree) {
                val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
                val parsed = CangJieLightParser.parseMacroCodeFragment(builder, seed)
                LightTreeRawCfirDeclarationBuilder(session, session.cangjieScopeProvider, parsed.tree, source,
                    macroFacts = parsed.macroFacts, nominalFacts = parsed.cjmpNominalFacts,
                    declarationModifiers = parsed.declarationModifiers, annotationMetadata = metadata)
                    .buildMacroFragmentPayload(parsed.roots, parsed.parsingInfo, snapshot)
            } else {
                val fragment = CjMacroCodeFragment(project, "primarySlice.cj", source, context = null, seed = seed)
                val contents = fragment.getContentElement()
                val parameter = assertIs<org.cangnova.cangjie.psi.CjParameter>(contents.roots.single())
                assertNull(parameter.ownerFunction)
                PsiRawCfirBuilder(session, annotationMetadata = metadata).buildMacroFragmentPayload(contents, snapshot)
            }
            val parameter = assertIs<MacroFragmentPayload.Root.Parameter>(checkNotNull(payload).roots.single()).parameter
            assertSame(constructor.symbol, parameter.containingDeclarationSymbol)
            val status = checkNotNull(parameter.memberParameterFieldInfo).createFieldStatus()
            assertEquals(listOf(false, !common, !common), listOf(status.isCommon, status.isSpecific, status.isVisibilityExplicit),
                "lightTree=$lightTree common=$common grammar=$grammar")
        }
    }
}
