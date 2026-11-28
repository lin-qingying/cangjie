package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentPayload
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentReparseResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawOwnerSnapshot
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.parsing.*
import org.cangnova.cangjie.psi.CjMacroCodeFragment
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/** 宏首个声明根的前缀与片段内修饰符共同决定 extend 属性。 */
@RunWith(JUnit3RunnerWithInners::class)
class CjmpMacroExtendPrefixRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiUsesSeedEntryRoleAfterParserRestoration() = checkSeedEntryRole(false)
    fun testLightTreeUsesSeedEntryRoleAfterParserRestoration() = checkSeedEntryRole(true)

    private fun checkSeedEntryRole(lightTree: Boolean) {
        val function = assertIs<CfirNamedFunction>(
            buildRoot(lightTree, "func example() {}", COMMON_ORIGIN, entryRole = CjmpFileRole.COMMON),
        )
        assertTrue(function.status.isCommon)
    }

    fun testBufferedFragmentReparsesInOwnerSessionAndReturnsDiagnostics() {
        val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
        val session = createTestSession(settings)
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
        val entry = CjMacroFragmentContext.Nodes(
            MacroParsingScope.Declaration(CjScopeKind.TOPLEVEL), CjMacroFragmentParent.OTHER, null,
            capturePrefix(SPECIFIC_ORIGIN),
        )
        val seed = createMacroFragmentParserSeed(
            ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), CjmpParsingContext.SyntaxOnly, entry,
        )
        val text = "func example() {}"
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val transaction = CangJieLightParser.parseMacroCodeFragmentBuffered(builder, seed)
        val owner = MacroRawOwnerSnapshot(FqName.ROOT, emptyList(), null, false, null, null, null, emptyList(), emptyList())
        val converter = LightTree2Cfir(session, session.cangjieScopeProvider)
        val result = assertIs<MacroFragmentReparseResult.SyntaxFailure>(
            converter.buildMacroFragment(transaction, CjmpFileRole.NONE, owner, CfirAnnotationMetadataBuilder()),
        )
        assertEquals(text, result.parsedText)
        assertTrue(result.diagnostics.isNotEmpty())
        assertFailsWith<IllegalStateException> {
            converter.buildMacroFragment(transaction, CjmpFileRole.NONE, owner, CfirAnnotationMetadataBuilder())
        }
    }

    fun testPsiCommonPrefix() = checkExtend(false, "extend Int64 {}", true, false)
    fun testLightTreeCommonPrefix() = checkExtend(true, "extend Int64 {}", true, false)
    fun testPsiConflictingPrefixAndLocalModifier() = checkExtend(false, "specific extend Int64 {}", false, false)
    fun testLightTreeConflictingPrefixAndLocalModifier() = checkExtend(true, "specific extend Int64 {}", false, false)
    fun testPsiRejectedSpecificPrefix() = checkRejectedSpecificPrefix(false)
    fun testLightTreeRejectedSpecificPrefix() = checkRejectedSpecificPrefix(true)
    fun testPsiCommonPrefixDoesNotReachMembers() = checkCommonPrefixDoesNotReachMembers(false)
    fun testLightTreeCommonPrefixDoesNotReachMembers() = checkCommonPrefixDoesNotReachMembers(true)
    fun testPsiSyntaxOnlyPrefixRequiresTransactionEntry() {
        assertFailsWith<IllegalArgumentException> {
            buildRoot(false, "func example() {}", SPECIFIC_ORIGIN, syntaxOnly = true)
        }
    }
    fun testLightTreeSyntaxOnlyPrefixRequiresTransactionEntry() {
        assertFailsWith<IllegalArgumentException> {
            buildRoot(true, "func example() {}", SPECIFIC_ORIGIN, syntaxOnly = true)
        }
    }

    private fun checkExtend(lightTree: Boolean, text: String, common: Boolean, specific: Boolean) {
        val extension = assertIs<CfirExtend>(buildRoot(lightTree, text, COMMON_ORIGIN))
        assertEquals(common, extension.status.isCommon)
        assertEquals(specific, extension.status.isSpecific)
    }

    private fun checkRejectedSpecificPrefix(lightTree: Boolean) {
        val function = assertIs<CfirNamedFunction>(buildRoot(lightTree, "func example() {}", SPECIFIC_ORIGIN))
        assertEquals(false, function.status.isSpecific)
    }

    private fun checkCommonPrefixDoesNotReachMembers(lightTree: Boolean) {
        val klass = assertIs<CfirClass>(buildRoot(lightTree, "class Example { func member() {} }", COMMON_ORIGIN))
        assertEquals(true, klass.status.isCommon)
        val member = klass.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertEquals(false, member.status.isCommon)
    }

    private fun buildRoot(
        lightTree: Boolean, text: String, prefixSource: String, syntaxOnly: Boolean = false,
        entryRole: CjmpFileRole = CjmpFileRole.NONE,
    ): org.cangnova.cangjie.cfir.declarations.CfirDeclaration {
        val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
        val session = createTestSession(settings)
        val cjmp = CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON)
        session.register(CfirCjmpSettingsComponent::class, cjmp)
        val entry = CjMacroFragmentContext.Nodes(
            MacroParsingScope.Declaration(CjScopeKind.TOPLEVEL), CjMacroFragmentParent.OTHER, null,
            capturePrefix(prefixSource),
        )
        val seed = createMacroFragmentParserSeed(
            ParsingFileFacts(CjSourceKind.SOURCE, entryRole),
            if (syntaxOnly) CjmpParsingContext.SyntaxOnly else cjmp.toParsingContext(settings), entry,
        )
        val owner = MacroRawOwnerSnapshot(FqName.ROOT, emptyList(), null, false, null, null, null, emptyList(), emptyList())
        val metadata = CfirAnnotationMetadataBuilder()
        val payload = if (lightTree) {
            val psiBuilder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
            val parsed = CangJieLightParser.parseMacroCodeFragment(psiBuilder, seed)
            assertEquals(0, parsed.parsingInfo.inheritedPrefixRootIndex)
            LightTreeRawCfirDeclarationBuilder(
                session = session, baseScopeProvider = session.cangjieScopeProvider, tree = parsed.tree,
                source = text, sourceKind = parsed.fileFacts.sourceKind, macroFacts = parsed.macroFacts,
                nominalFacts = parsed.cjmpNominalFacts, declarationModifiers = parsed.declarationModifiers,
                annotationMetadata = metadata,
            ).buildMacroFragmentPayload(parsed.roots, parsed.parsingInfo, owner)
        } else {
            val fragment = CjMacroCodeFragment(project, "macro.cj", text, context = null, seed = seed)
            val contents = fragment.getContentElement()
            assertEquals(0, contents.parsingInfo.inheritedPrefixRootIndex)
            PsiRawCfirBuilder(session, annotationMetadata = metadata).buildMacroFragmentPayload(contents, owner)
        }
        val root = assertIs<MacroFragmentPayload.Root.Declaration>(checkNotNull(payload).roots.single())
        return root.declaration
    }

    /** 从独立原始文件捕获 parser 对象，再交给 PSI/LT 构造；不从 CFIR status 重建前缀来源。 */
    private fun capturePrefix(text: String): CjMacroDeclarationPrefix {
        val sourceFile = CjInMemoryTextSourceFile("raw-prefix-origin.cj", "test-origins/raw-prefix-origin.cj", text)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val parsed = CangJieLightParser.parseBuffered(builder, sourceFile = sourceFile)
        assertEquals(emptyList<ParsingDiagnostic>(), parsed.diagnostics)
        return parsed.file.declarationPrefixes.getValue(text.indexOf("func origin"))
    }

    private companion object {
        const val COMMON_ORIGIN = "common func origin(): Unit {}"
        const val SPECIFIC_ORIGIN = "specific func origin(): Unit {}"
    }
}
