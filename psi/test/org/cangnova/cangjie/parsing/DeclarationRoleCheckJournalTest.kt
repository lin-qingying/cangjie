package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.CjmpFileRoleError

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleModifiers
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** 使用真实 builder marker 检查声明事实与语法树的事务边界。 */
class DeclarationRoleCheckJournalTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun rollbackRestoresCommonRoleTogetherWithItsEvents() {
        assertParserRoleRollback("common")
    }

    @Test
    fun rollbackRestoresSpecificRoleTogetherWithItsEvents() {
        assertParserRoleRollback("specific")
    }

    private fun assertParserRoleRollback(role: String) {
        val source = "$role func candidate(): Unit {}"
        val builder = SemanticWhitespaceAwarePsiBuilderImpl(
            PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source),
        )
        val context = CjmpParsingContext.Compilation(true, true, true)
        val parser = CangJieParsing.createForTopLevelNonLazy(builder, cjmpContext = context)
        val before = parser.parserSeed(AbstractCangJieParsing.ParsingContext.DEFAULT)
        val probe = builder.mark()
        parser.parseFile()
        assertEquals(if (role == "common") CjmpFileRole.COMMON else CjmpFileRole.SPECIFIC,
            parser.parsedFileFacts.cjmpRole)
        probe.rollbackTo()
        assertEquals(before, parser.parserSeed(AbstractCangJieParsing.ParsingContext.DEFAULT))
        assertEquals(emptyList<CjmpParsedRoleEvent>(), builder.macroParsingJournal.freeze(0).roleEvents)
    }

    @Test
    fun newTokenStreamDoesNotInheritAnotherRootsRoleState() {
        fun builder(source: String) = SemanticWhitespaceAwarePsiBuilderImpl(
            PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source),
        )
        val original = CangJieParsing.createForTopLevelNonLazy(
            builder("specific func first(): Unit {}"), cjmpContext = CjmpParsingContext.Compilation(true, true, true),
        )
        original.parseFile()
        val independent = original.create(builder("common func second(): Unit {}"))
        assertEquals(CjmpFileRole.NONE, independent.parsedFileFacts.cjmpRole)
        independent.parseFile()
        assertEquals(CjmpFileRole.COMMON, independent.parsedFileFacts.cjmpRole)
        assertEquals(CjmpFileRole.SPECIFIC, original.parsedFileFacts.cjmpRole)
    }

    @Test
    fun rejectedCheckKeepsItsPartialWriteUntilItsProductionIsRolledBack() {
        val builder = semanticBuilder("common specific func candidate(): Unit {}")
        val parser = CangJieParsing.createForTopLevelNonLazy(
            builder, cjmpContext = CjmpParsingContext.Compilation(true, true, true),
        )
        val before = builder.macroParsingJournal.fileState
        val probe = builder.mark()
        parser.parseFile()
        val failed = builder.macroParsingJournal.fileState
        assertEquals(CjmpFileRole.SPECIFIC, failed.role)
        assertEquals(CjmpFileRoleError.COMMON_AND_SPECIFIC, failed.error?.roleError)
        probe.rollbackTo()
        assertEquals(before, builder.macroParsingJournal.fileState)
        parser.parseFile()
        assertEquals(failed, builder.macroParsingJournal.fileState)
        assertEquals(CjmpFileRole.SPECIFIC, failed.role)
    }

    @Test
    fun truncatedParserSharesTheRootStateAndCheckpoint() {
        val source = "common func candidate(): Unit {}"
        val builder = semanticBuilder(source)
        val parser = CangJieParsing.createForTopLevelNonLazy(
            builder, cjmpContext = CjmpParsingContext.Compilation(true, true, true),
        )
        val child = parser.create(TruncatedSemanticWhitespaceAwarePsiBuilder(builder, source.length))
        val probe = builder.mark()
        child.parseFile()
        assertEquals(CjmpFileRole.COMMON, parser.parsedFileFacts.cjmpRole)
        assertEquals(parser.parsedFileFacts, child.parsedFileFacts)
        probe.rollbackTo()
        assertEquals(CjmpFileRole.NONE, parser.parsedFileFacts.cjmpRole)
        assertEquals(parser.parsedFileFacts, child.parsedFileFacts)
    }

    @Test
    fun quoteScopeRestoresItsEntryDepthAfterAnOuterRollback() {
        val builder = semanticBuilder("")
        val parser = CangJieParsing.createForTopLevelNonLazy(builder)
        val probe = builder.mark()
        parser.withQuotedFileSyntax {
            assertEquals(1, builder.macroParsingJournal.fileState.quotedSyntaxDepth)
            parser.withQuotedFileSyntax {
                assertEquals(2, builder.macroParsingJournal.fileState.quotedSyntaxDepth)
                probe.rollbackTo()
                assertEquals(0, builder.macroParsingJournal.fileState.quotedSyntaxDepth)
            }
            assertEquals(1, builder.macroParsingJournal.fileState.quotedSyntaxDepth)
        }
        assertEquals(0, builder.macroParsingJournal.fileState.quotedSyntaxDepth)
    }

    @Test
    fun rollbackPreservesTheIndependentRootsFullSeed() {
        val builder = semanticBuilder("common func candidate(): Unit {}")
        val context = CjmpParsingContext.Compilation(true, true, true, initialFileRole = CjmpFileRole.COMMON)
        val parser = CangJieParsing.createForTopLevelNonLazy(builder, cjmpContext = context)
        val seed = parser.parserSeed(AbstractCangJieParsing.ParsingContext.DEFAULT)
            .copy(fileRole = CjmpFileRole.SPECIFIC, quotedSyntaxDepth = 2)
        parser.restoreSeed(seed)
        val probe = builder.mark()
        parser.parseFile()
        probe.rollbackTo()
        assertEquals(seed, parser.parserSeed(AbstractCangJieParsing.ParsingContext.DEFAULT))
    }

    @Test
    fun rollbackRestoresTheParsedModuleTogetherWithTheRole() {
        val builder = semanticBuilder("package owner.member\ncommon func candidate(): Unit {}")
        val parser = CangJieParsing.createForTopLevelNonLazy(
            builder, languageModuleName = "initial", cjmpContext = CjmpParsingContext.Compilation(true, true, true),
        )
        val before = parser.parserSeed(AbstractCangJieParsing.ParsingContext.DEFAULT)
        val probe = builder.mark()
        parser.parseFile()
        assertEquals("owner", parser.languageModuleName)
        probe.rollbackTo()
        assertEquals(before, parser.parserSeed(AbstractCangJieParsing.ParsingContext.DEFAULT))
    }

    @Test
    fun truncatedParserPublishesTheModuleToItsRoot() {
        val source = "package owner.member\ncommon func candidate(): Unit {}"
        val builder = semanticBuilder(source)
        val parser = CangJieParsing.createForTopLevelNonLazy(
            builder, languageModuleName = "initial", cjmpContext = CjmpParsingContext.Compilation(true, true, true),
        )
        val child = parser.create(TruncatedSemanticWhitespaceAwarePsiBuilder(builder, source.length))
        child.parseFile()
        assertEquals("owner", child.languageModuleName)
        assertEquals("owner", parser.languageModuleName)
    }

    private fun semanticBuilder(source: String) = SemanticWhitespaceAwarePsiBuilderImpl(
        PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source),
    )

    @Test
    fun rollbackAndCollapseRestoreDeclarationFacts() {
        val builder = SemanticWhitespaceAwarePsiBuilderImpl(
            PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), "func f() {}"),
        )
        val journal = builder.macroParsingJournal
        journal.registerDeclarationRoleCheck(0, CjmpDeclarationRoleCheck.NOT_CHECKED)
        journal.registerDeclarationRoleCheck(11, CjmpDeclarationRoleCheck.ACCEPTED)
        val outer = builder.mark()
        val child = builder.mark()
        journal.registerDeclarationRoleCheck(5, CjmpDeclarationRoleCheck.REJECTED)
        while (!builder.eof()) builder.advanceLexer()
        child.collapse(CjNodeTypes.BLOCK)
        assertNull(journal.declarationRoleCheckAt(0))
        assertNull(journal.declarationRoleCheckAt(5))
        assertEquals(CjmpDeclarationRoleCheck.ACCEPTED, journal.declarationRoleCheckAt(11))
        outer.rollbackTo()
        assertEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, journal.declarationRoleCheckAt(0))
        assertEquals(CjmpDeclarationRoleCheck.ACCEPTED, journal.declarationRoleCheckAt(11))
        assertNull(journal.declarationRoleCheckAt(5))
        val retry = builder.mark()
        journal.registerDeclarationRoleCheck(5, CjmpDeclarationRoleCheck.ACCEPTED)
        retry.drop()
        assertEquals(CjmpDeclarationRoleCheck.ACCEPTED, journal.declarationRoleCheckAt(5))
    }

    @Test
    fun freezeCopiesAndProtectsAllThreeStates() {
        val journal = MacroParsingJournal()
        journal.registerDeclarationRoleCheck(0, CjmpDeclarationRoleCheck.NOT_CHECKED)
        journal.registerDeclarationRoleCheck(1, CjmpDeclarationRoleCheck.ACCEPTED)
        journal.registerDeclarationRoleCheck(2, CjmpDeclarationRoleCheck.REJECTED)
        val frozen = journal.freeze(0).declarationRoleChecks
        assertEquals(mapOf(0 to CjmpDeclarationRoleCheck.NOT_CHECKED, 1 to CjmpDeclarationRoleCheck.ACCEPTED,
            2 to CjmpDeclarationRoleCheck.REJECTED), frozen)
        assertFailsWith<UnsupportedOperationException> {
            (frozen as MutableMap<Int, CjmpDeclarationRoleCheck>).clear()
        }
        assertFailsWith<IllegalStateException> {
            journal.registerDeclarationRoleCheck(3, CjmpDeclarationRoleCheck.ACCEPTED)
        }
        assertFailsWith<IllegalStateException> { journal.rollback(0) }
        assertEquals(3, frozen.size)
    }

    @Test
    fun roleEventsKeepOrderAndRollbackCollapsedUncertainty() {
        val journal = MacroParsingJournal()
        val common = CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false))
        val specific = CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false))
        val retainedId = journal.registerRoleEvent(20, common)
        val checkpoint = journal.checkpoint
        val discardedId = journal.registerRoleEvent(10, specific)
        journal.discardCollapsedChildren(0, 30)
        journal.rollback(checkpoint)
        val retriedId = journal.registerRoleEvent(10, specific)
        val frozen = journal.freeze(0).roleEvents
        assertEquals(listOf(20, 10), frozen.map { it.declarationOffset })
        assertEquals(listOf(common, specific), frozen.map { it.kind })
        assertEquals(listOf(retainedId, retriedId), frozen.map { it.id })
        kotlin.test.assertTrue(retriedId.sequence > discardedId.sequence)
        assertEquals(frozen.first().id.rootIdentity, frozen.last().id.rootIdentity)
        assertFailsWith<UnsupportedOperationException> { (frozen as MutableList<CjmpParsedRoleEvent>).clear() }
    }

    @Test
    fun rollbackRestoresMultipleSurvivingEventsInTheirOriginalCheckOrder() {
        val journal = MacroParsingJournal()
        val common = CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false))
        val specific = CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false))
        val first = journal.registerRoleEvent(20, common)
        val second = journal.registerRoleEvent(10, specific)
        val checkpoint = journal.checkpoint
        journal.discardCollapsedChildren(0, 30)
        journal.rollback(checkpoint)
        val frozen = journal.freeze(0).roleEvents
        assertEquals(listOf(first, second), frozen.map { it.id })
        assertEquals(listOf(20, 10), frozen.map { it.declarationOffset })
        assertEquals(listOf(common, specific), frozen.map { it.kind })
    }
}
