package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjMacroCodeFragment
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CjmpMacroDeclarationRoleParsingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun bufferedMacroParseRetainsItsSeedInputAndDiagnosticOwnership() {
        val source = "specific func first() {}\r\ncommon func second() {}"
        val context = CjmpParsingContext.Compilation(true, false, true)
        val entry = CjMacroFragmentContext.Nodes(
            MacroParsingScope.Declaration(CjScopeKind.TOPLEVEL), CjMacroFragmentParent.OTHER, null,
            CjMacroDeclarationPrefix.EMPTY,
        )
        val sourceContext = CjParserSourceContext.complete(source)
        val seed = createMacroFragmentParserSeed(ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), context, entry)
            .copy(sourceContext = sourceContext, lexicalInput = CjLexicalSourceInput(sourceContext.snapshot))
        fun builder() = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)

        val buffered = CangJieLightParser.parseMacroCodeFragmentBuffered(builder(), seed)
        val delivered = mutableListOf<ParsingDiagnostic>()
        val external = CangJieLightParser.parseMacroCodeFragment(builder(), seed) { delivered.add(it) }
        val unobserved = CangJieLightParser.parseMacroCodeFragment(builder(), seed)

        assertEquals(CangJieLightDiagnosticOwnership.EXTERNAL_LISTENER, external.diagnosticOwnership)
        assertEquals(CangJieLightDiagnosticOwnership.UNOBSERVED, unobserved.diagnosticOwnership)
        buffered.consume { fragment, diagnostics ->
            assertSame(seed, fragment.inputSeed)
            assertEquals(source, fragment.inputText)
            assertEquals(CangJieLightDiagnosticOwnership.BUFFERED, fragment.diagnosticOwnership)
            assertTrue(diagnostics.isNotEmpty())
            assertEquals(delivered, diagnostics)
            assertEquals(external.declarationRoleChecks, fragment.declarationRoleChecks)
        }
        assertFailsWith<IllegalStateException> { buffered.consume { _, _ -> } }
        val failed = CangJieLightParser.parseMacroCodeFragmentBuffered(builder(), seed)
        assertFailsWith<IllegalArgumentException> { failed.consume { _, _ -> throw IllegalArgumentException("construction failed") } }
        assertFailsWith<IllegalStateException> { failed.consume { _, _ -> } }
    }

    @Test
    fun bufferedMacroReparseUsesExplicitOwnerRoleAndDiscardsOldDiagnostics() {
        val source = "func example() {}\r\n"
        val entry = CjMacroFragmentContext.Nodes(
            MacroParsingScope.Declaration(CjScopeKind.TOPLEVEL), CjMacroFragmentParent.OTHER, null,
            capturePrefix("specific func origin(): Unit {}"),
        )
        val seed = createMacroFragmentParserSeed(
            ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), CjmpParsingContext.SyntaxOnly, entry,
        )
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val original = CangJieLightParser.parseMacroCodeFragmentBuffered(builder, seed)
        val target = CjmpParsingContext.Compilation(compileCommon = true, compileSpecific = false, enabled = true)
        assertFailsWith<IllegalArgumentException> { original.reparse(target, CjmpFileRole.COMMON) }
        val reparsed = original.reparse(target.copy(initialFileRole = CjmpFileRole.COMMON), CjmpFileRole.COMMON)
        assertFailsWith<IllegalStateException> { original.consume { _, _ -> } }
        reparsed.consume { fragment, diagnostics ->
            assertEquals(source, fragment.inputText)
            assertEquals(entry, fragment.inputSeed.macroFragment)
            assertEquals(CjmpFileRole.COMMON, fragment.inputSeed.fileRole)
            assertEquals(CjmpFileRole.COMMON, fragment.parsingInfo.entryFileRole)
            assertEquals(CjmpDeclarationRoleCheck.REJECTED, fragment.parsingInfo.inheritedPrefixRoleCheck)
            assertTrue(diagnostics.isNotEmpty())
        }
        assertFailsWith<IllegalStateException> { reparsed.consume { _, _ -> } }
    }

    @Test
    fun bufferedMacroReparseDoesNotCarryRejectedDiagnosticsIntoAcceptedContext() {
        val source = "func example() {}"
        val entry = CjMacroFragmentContext.Nodes(
            MacroParsingScope.Declaration(CjScopeKind.TOPLEVEL), CjMacroFragmentParent.OTHER, null,
            capturePrefix("specific func origin(): Unit {}"),
        )
        val oldContext = CjmpParsingContext.Compilation(compileCommon = true, compileSpecific = false, enabled = true)
        val seed = createMacroFragmentParserSeed(ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), oldContext, entry)
        fun parse() = CangJieLightParser.parseMacroCodeFragmentBuffered(
            PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source), seed,
        )
        parse().consume { fragment, diagnostics ->
            assertEquals(CjmpDeclarationRoleCheck.REJECTED, fragment.parsingInfo.inheritedPrefixRoleCheck)
            assertTrue(diagnostics.isNotEmpty())
        }
        val target = CjmpParsingContext.Compilation(compileCommon = false, compileSpecific = true, enabled = true)
        parse().reparse(target, CjmpFileRole.NONE).consume { fragment, diagnostics ->
            assertEquals(CjmpDeclarationRoleCheck.ACCEPTED, fragment.parsingInfo.inheritedPrefixRoleCheck)
            assertTrue(diagnostics.isEmpty())
            assertEquals(source, fragment.inputText)
        }
    }

    @Test
    fun bufferedMacroKeepsGenerationWhenSeedRoleOverridesContextDefault() {
        val source = "func example() {}"
        val entry = CjMacroFragmentContext.Nodes(
            MacroParsingScope.Declaration(CjScopeKind.TOPLEVEL), CjMacroFragmentParent.OTHER, null,
            capturePrefix("common func origin(): Unit {}"),
        )
        val context = CjmpParsingContext.Compilation(compileCommon = true, compileSpecific = false, enabled = true)
        val seed = createMacroFragmentParserSeed(ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.COMMON), context, entry)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val transaction = CangJieLightParser.parseMacroCodeFragmentBuffered(builder, seed)
        transaction.forContext(context.copy(initialFileRole = CjmpFileRole.COMMON), CjmpFileRole.COMMON)
            .consume { fragment, diagnostics ->
                assertSame(seed, fragment.inputSeed)
                assertEquals(CjmpFileRole.COMMON, fragment.parsingInfo.entryFileRole)
                assertEquals(CjmpDeclarationRoleCheck.ACCEPTED, fragment.parsingInfo.inheritedPrefixRoleCheck)
                assertTrue(diagnostics.isEmpty())
            }
        assertFailsWith<IllegalStateException> { transaction.consume { _, _ -> } }
    }

    @Test
    fun macroInputUsesDeclarationKeywordAsAnchor() {
        val source = "@Wrap common func target(): Unit {}"
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val facts = CangJieLightParser.parse(
            builder, cjmpContext = CjmpParsingContext.Compilation(false, true, true),
        ).declarationRoleChecks
        assertEquals(mapOf(source.indexOf("func") to CjmpDeclarationRoleCheck.REJECTED), facts)
    }

    @Test
    fun scopedDeclarationsKeepRoleAndKeywordAnchors() {
        val source = "common func first() {}\nfunc second() {}"
        val first = source.indexOf("func first")
        val second = source.indexOf("func second")
        assertEquals(
            mapOf(first to CjmpDeclarationRoleCheck.REJECTED, second to CjmpDeclarationRoleCheck.NOT_CHECKED),
            fragmentFacts(source, CjScopeKind.TOPLEVEL, CjmpParsingContext.Compilation(false, true, true)),
        )
        assertEquals(
            mapOf(first to CjmpDeclarationRoleCheck.NOT_CHECKED, second to CjmpDeclarationRoleCheck.NOT_CHECKED),
            fragmentFacts(source, CjScopeKind.TOPLEVEL, CjmpParsingContext.SyntaxOnly),
        )
    }

    @Test
    fun enumConstructorScopeDoesNotCheckFileRole() {
        val source = "common A"
        assertEquals(
            mapOf(source.indexOf("A") to CjmpDeclarationRoleCheck.NOT_CHECKED),
            fragmentFacts(source, CjScopeKind.ENUM_CONSTRUCTOR, CjmpParsingContext.Compilation(false, true, true)),
        )
        assertEquals(
            mapOf(0 to CjmpDeclarationRoleCheck.NOT_CHECKED),
            fragmentFacts("A", CjScopeKind.ENUM_CONSTRUCTOR, CjmpParsingContext.SyntaxOnly),
        )
    }

    @Test
    fun unknownScopeDoesNotCheckFileRole() {
        val source = "common func value() {}"
        assertEquals(
            mapOf(source.indexOf("func") to CjmpDeclarationRoleCheck.NOT_CHECKED),
            fragmentFacts(source, CjScopeKind.UNKNOWN_SCOPE, CjmpParsingContext.Compilation(false, true, true)),
        )
    }

    @Test
    fun enumMacroInputUsesConstructorNameAsAnchor() {
        val source = "enum E { | @Wrap common A }"
        assertEquals(
            mapOf(source.indexOf("enum") to CjmpDeclarationRoleCheck.NOT_CHECKED,
                source.lastIndexOf("A") to CjmpDeclarationRoleCheck.NOT_CHECKED),
            fileFacts(source, CjmpParsingContext.Compilation(false, true, true)),
        )
    }

    @Test
    fun foreignFunctionHasItsOwnRoleFact() {
        val source = "foreign { common func f(): Unit }"
        val root = source.indexOf("foreign")
        val function = source.indexOf("func")
        assertEquals(
            mapOf(root to CjmpDeclarationRoleCheck.NOT_CHECKED, function to CjmpDeclarationRoleCheck.REJECTED),
            fileFacts(source, CjmpParsingContext.Compilation(false, true, true)),
        )
        assertEquals(
            mapOf(root to CjmpDeclarationRoleCheck.NOT_CHECKED, function to CjmpDeclarationRoleCheck.NOT_CHECKED),
            fileFacts(source, CjmpParsingContext.SyntaxOnly),
        )
    }

    @Test
    fun scopedSeparatorRecoveryDoesNotRegisterUnparsedTail() {
        val source = "func first() {} func second() {}"
        assertEquals(
            mapOf(source.indexOf("func first") to CjmpDeclarationRoleCheck.NOT_CHECKED),
            fragmentFacts(source, CjScopeKind.TOPLEVEL, CjmpParsingContext.Compilation(false, true, true)),
        )
    }

    @Test
    fun fragmentRetainsItsEntryRoleAndParsingContext() {
        val source = "common func value() {}"
        val context = CjmpParsingContext.Compilation(true, true, true)
        val entry = CjMacroFragmentContext.Nodes(
            MacroParsingScope.Declaration(CjScopeKind.TOPLEVEL), CjMacroFragmentParent.OTHER, null, CjMacroDeclarationPrefix.EMPTY,
        )
        val seed = createMacroFragmentParserSeed(ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.SPECIFIC), context, entry)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val light = CangJieLightParser.parseMacroCodeFragment(builder, seed)
        val psi = CjMacroCodeFragment(project, "fragment.cj", source, null, seed).getContentElement()
        for (info in listOf(light.parsingInfo, psi.parsingInfo)) {
            assertEquals(context, info.parsingContext)
            assertEquals(CjmpFileRole.SPECIFIC, info.entryFileRole)
            assertEquals(CjmpDeclarationRoleCheck.REJECTED, info.inheritedPrefixRoleCheck)
        }
    }

    /** 独立原始文件的 SyntaxOnly 解析提供带来源的前缀，目标文件角色仍由各用例自己的 seed 决定。 */
    private fun capturePrefix(text: String): CjMacroDeclarationPrefix {
        val sourceFile = CjInMemoryTextSourceFile("role-prefix-origin.cj", "test-origins/role-prefix-origin.cj", text)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val parsed = CangJieLightParser.parseBuffered(builder, sourceFile = sourceFile)
        assertEquals(emptyList<ParsingDiagnostic>(), parsed.diagnostics)
        return parsed.file.declarationPrefixes.getValue(text.indexOf("func origin"))
    }

    private fun fileFacts(source: String, context: CjmpParsingContext): Map<Int, CjmpDeclarationRoleCheck> {
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        return CangJieLightParser.parse(builder, cjmpContext = context).declarationRoleChecks
    }

    private fun fragmentFacts(
        source: String,
        scope: CjScopeKind,
        context: CjmpParsingContext,
    ): Map<Int, CjmpDeclarationRoleCheck> {
        val entry = CjMacroFragmentContext.Nodes(
            MacroParsingScope.Declaration(scope), CjMacroFragmentParent.OTHER, null, CjMacroDeclarationPrefix.EMPTY,
        )
        val seed = createMacroFragmentParserSeed(ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), context, entry)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        return CangJieLightParser.parseMacroCodeFragment(builder, seed).declarationRoleChecks
    }
}
