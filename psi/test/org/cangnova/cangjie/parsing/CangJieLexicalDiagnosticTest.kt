package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.impl.source.tree.LazyParseableElement
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.CjParsingDiagnosticRange
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull as presentValue
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** 普通表达式词法状态的未知起始字符在 PSI 与 LightTree 中共用一份仓颉词法诊断规则。 */
class CangJieLexicalDiagnosticTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun unknownCharactersInQuoteInputAndAttrsKeepTheirStartPointsAndNativeRanges() {
        assertBothRoutes("func host() { let value = quote(§) }", "§")
        assertBothRoutes("func host() { let value = @Input(😀) }", "😀")
        assertBothRoutes("func host() { let value = @Attr[§]() }", "§")
    }

    @Test
    fun nativeCharacterDisplayUsesCodePointsAndControlEscapes() {
        assertBothRoutes("func host() { let value = quote(😀) }", "😀")
        assertBothRoutes("func host() { let value = quote(\u0001) }", "\\u{0001}", actualText = "\u0001")
        assertBothRoutes("func host() { let value = quote(\r) }", "\\u{000D}", actualText = "\r")
    }

    @Test
    fun commentsAndStringContentsDoNotBecomeUnknownStartDiagnostics() {
        val source = "func host() { let text = \"§\" // 😀\nlet value = @M(1) }"
        assertBothRoutes(source)
    }

    @Test
    fun lookaheadAndMarkerRollbackKeepOneDiagnosticForTheCachedLexicalToken() {
        val source = "func host() { let value = quote(§) }"
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            CangJieParserDefinition(), CangJieLexer(), source,
        )
        builder.setDebugMode(true)
        repeat(4) { builder.lookAhead(it) }
        val speculative = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        speculative.rollbackTo()

        val parsed = CangJieLightParser.parseBuffered(builder)
        assertEquals(listOf(expected(parsed.file.sourceContext.snapshot, "§")), parsed.diagnostics)
    }

    @Test
    fun recursivePsiCollectionReportsUnknownCharactersInsideARealLazyBlock() {
        val source = "func host() { let value = quote(§) }"
        val file = CjPsiFactory(project, markGenerated = false).createFile(source)
        val lazyBlock = presentValue(findLazyBlock(file.node), "the test must exercise an SDK-lazy CangJie block")
        assertFalse(lazyBlock.isParsed, "the block must still be lazy before diagnostic collection")
        val diagnostics = file.collectParsingDiagnostics()
        val snapshot = presentValue(file.parsingSourceContextOrNull()).snapshot
        assertEquals(listOf(expected(snapshot, "§")), diagnostics)
        assertTrue(lazyBlock.isParsed, "recursive diagnostic collection must parse the lazy block")
    }

    private fun assertBothRoutes(source: String, display: String? = null, actualText: String = display ?: "") {
        val file = CjPsiFactory(project, markGenerated = false).createFile(source)
        val psiDiagnostics = file.collectParsingDiagnostics()
        val input = presentValue(presentValue(file.getCopyableUserData(ROOT_PARSER_SEED_KEY)).lexicalInput)
        val expected = if (display == null) emptyList() else listOf(expected(input.source, display, actualText))
        assertEquals(expected, psiDiagnostics, "PSI\n$source")

        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val parsed = CangJieLightParser.parseBuffered(builder, lexicalInput = input)
        assertSame(input, parsed.file.lexicalInput)
        assertSame(input.source, parsed.file.sourceContext.snapshot)
        assertEquals(expected, parsed.diagnostics, "LightTree\n$source")
    }

    /** 官方 LexerDiag.cpp 的 Position 重载：UTF-16 起点不扩为非法字符范围，主提示仅宽一原生列。 */
    private fun expected(source: CjParsingSourceSnapshot, displayedText: String, actualText: String = displayedText): ParsingDiagnostic {
        val start = source.text.indexOf(actualText)
        assertTrue(start >= 0, "source character not found: $actualText")
        val begin = source.nativePosition(start)
        return ParsingDiagnostic(
            start,
            start,
            CangJieParsingBundle.message("parsing.error.unknown.token.start", displayedText),
            category = ParsingDiagnostic.Category.LEX,
            rangeKind = ParsingDiagnostic.RangeKind.POINT,
            mainRange = CjParsingDiagnosticRange(source, begin, begin.first to begin.second + 1),
        )
    }

    private fun findLazyBlock(node: com.intellij.lang.ASTNode): LazyParseableElement? {
        if (node.elementType == org.cangnova.cangjie.psi.CjNodeTypes.BLOCK && node is LazyParseableElement) return node
        var child = node.firstChildNode
        while (child != null) {
            findLazyBlock(child)?.let { return it }
            child = child.treeNext
        }
        return null
    }
}
