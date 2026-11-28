package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.SyntaxTreeBuilder
import com.intellij.psi.TokenType
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as equal
import kotlin.test.assertNull as absent
import kotlin.test.assertNotEquals as different
import kotlin.test.assertFailsWith

/** 用真实 SDK builder 验证 speculative marker 对语法事实的保留/撤销，不模拟 marker 池。 */
class MacroParsingJournalMarkerTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    private val original = MacroParsingFacts(
        MacroParsingScope.Declaration(CjScopeKind.UNKNOWN_SCOPE), CjMacroFragmentParent.OTHER, "`Owner`",
    )

    @Test
    fun rollbackRestoresRegistrationAndBothRefinements() {
        val builder = builder("@M()")
        val root = builder.mark()
        val journal = builder.macroParsingJournal
        journal.register(0, original)
        val speculation = builder.mark()
        journal.refine(0) { it.copy(scope = MacroParsingScope.Expression(CjExprKind.VAR_INIT)) }
        journal.refine(0) { it.copy(parent = CjMacroFragmentParent.TUPLE_LITERAL) }
        builder.advanceLexer()
        speculation.rollbackTo()
        equal(original, journal.factsAt(0))
        root.rollbackTo()
        absent(journal.factsAt(0))
        // SDK 已归池 marker；同一 AT 的实际重解析只能产生新注册。
        val reparsed = builder.mark()
        journal.register(0, original.copy(scope = MacroParsingScope.Declaration(CjScopeKind.MACRO_BODY)))
        reparsed.drop()
        equal(CjScopeKind.MACRO_BODY, (journal.factsAt(0)!!.scope as MacroParsingScope.Declaration).kind)
    }

    @Test
    fun precedeUsesOriginalCheckpointAfterDoneAndDropKeepsChildFacts() {
        val builder = builder("@M()")
        val initial = builder.mark()
        val journal = builder.macroParsingJournal
        journal.register(0, original)
        builder.advanceLexer()
        initial.done(CjNodeTypes.REFERENCE_EXPRESSION)
        val preceding = initial.precede()
        journal.refine(0) { it.copy(parent = CjMacroFragmentParent.TUPLE_LITERAL) }
        preceding.rollbackTo()
        equal(0, builder.currentOffset)
        absent(journal.factsAt(0))
        val kept = builder.mark()
        journal.register(0, original)
        kept.drop()
        equal(original, journal.factsAt(0))
    }

    @Test
    fun collapseDeletionParticipatesInOuterRollback() {
        val builder = builder("@M()")
        val journal = builder.macroParsingJournal
        journal.register(0, original)
        val outer = builder.mark()
        val collapsed = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        collapsed.collapse(CjNodeTypes.BLOCK)
        absent(journal.factsAt(0))
        outer.rollbackTo()
        equal(original, journal.factsAt(0))
    }

    @Test
    fun truncatedViewSharesRootTransaction() {
        val builder = builder("@M() trailing")
        val truncated = TruncatedSemanticWhitespaceAwarePsiBuilder(builder, 4)
        val marker = truncated.mark()
        builder.macroParsingJournal.register(0, original)
        while (!truncated.eof()) truncated.advanceLexer()
        marker.rollbackTo()
        equal(0, builder.currentOffset)
        absent(builder.macroParsingJournal.factsAt(0))
    }

    @Test
    fun beforeOverloadsUnwrapAndProductionGettersRetainSdkRanges() {
        for (syntaxInterface in listOf(false, true)) {
            val builder = builder("@M()")
            val root = builder.mark()
            val first = builder.mark()
            builder.advanceLexer()
            val before = builder.mark()
            if (syntaxInterface) {
                (first as SyntaxTreeBuilder.Marker).doneBefore(CjNodeTypes.REFERENCE_EXPRESSION, before)
            } else {
                first.doneBefore(CjNodeTypes.REFERENCE_EXPRESSION, before)
            }
            before.drop()
            while (!builder.eof()) builder.advanceLexer()
            root.done(CjNodeTypes.CJ_FILE)
            builder.lightTree
            equal(CjNodeTypes.REFERENCE_EXPRESSION, first.tokenType)
            equal(0, first.startOffset)
            equal(1, first.endOffset)
            equal(0, first.startIndex)
            equal(1, first.endIndex)
            absent(first.errorMessage)
            equal(false, first.isCollapsed)
        }
    }

    @Test
    fun rejectsBeforeMarkerFromAnotherRoot() {
        val first = builder("@M()").mark()
        val other = builder("@N()").mark()
        assertFailsWith<IllegalStateException> { first.doneBefore(CjNodeTypes.REFERENCE_EXPRESSION, other) }
    }

    @Test
    fun beforeErrorOverloadsKeepFactsAndExposeSdkErrorProduction() {
        for (syntaxInterface in listOf(false, true)) {
            for (errorNode in listOf(false, true)) {
                val builder = builder("@M()")
                val root = builder.mark()
                val first = builder.mark()
                builder.macroParsingJournal.register(0, original)
                builder.advanceLexer()
                val before = builder.mark()
                val message = "marker transaction error"
                if (syntaxInterface) {
                    val production: SyntaxTreeBuilder.Marker = first
                    val boundary: SyntaxTreeBuilder.Marker = before
                    if (errorNode) production.errorBefore(message, boundary)
                    else production.doneBefore(CjNodeTypes.REFERENCE_EXPRESSION, boundary, message)
                } else {
                    if (errorNode) first.errorBefore(message, before)
                    else first.doneBefore(CjNodeTypes.REFERENCE_EXPRESSION, before, message)
                }
                before.drop()
                while (!builder.eof()) builder.advanceLexer()
                root.done(CjNodeTypes.CJ_FILE)
                builder.lightTree
                equal(original, builder.macroParsingJournal.factsAt(0))
                equal(if (errorNode) TokenType.ERROR_ELEMENT else CjNodeTypes.REFERENCE_EXPRESSION, first.tokenType)
                equal(0, first.startOffset)
                equal(1, first.endOffset)
                if (errorNode) equal(message, first.errorMessage)
                else absent(first.errorMessage)
            }
        }
    }

    @Test
    fun collapsedProductionRetainsSdkFlagAndErrorKeepsFacts() {
        val builder = builder("@M()")
        val root = builder.mark()
        val error = builder.mark()
        builder.macroParsingJournal.register(0, original)
        builder.advanceLexer()
        error.error("macro error")
        equal(original, builder.macroParsingJournal.factsAt(0))
        val collapsed = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        collapsed.collapse(CjNodeTypes.BLOCK)
        root.done(CjNodeTypes.CJ_FILE)
        builder.lightTree
        equal("macro error", error.errorMessage)
        equal(true, collapsed.isCollapsed)
        equal(1, collapsed.startOffset)
        equal(4, collapsed.endOffset)
        equal(original, builder.macroParsingJournal.factsAt(0))
    }

    @Test
    fun unknownScopeIsDistinctFromUnknownExpressionAndCommaIsIndependent() {
        val expression = original.copy(scope = MacroParsingScope.Expression(CjExprKind.UNKNOWN_EXPR))
        different(original, expression)
        equal(original.scope, original.copy(parent = CjMacroFragmentParent.TUPLE_LITERAL).scope)
    }

    private fun builder(text: String): SemanticWhitespaceAwarePsiBuilderImpl =
        SemanticWhitespaceAwarePsiBuilderImpl(
            PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text),
        )
}
