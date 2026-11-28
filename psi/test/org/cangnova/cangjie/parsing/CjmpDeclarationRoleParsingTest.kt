package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleModifiers
import org.cangnova.cangjie.CjmpFileRoleMode
import org.cangnova.cangjie.CjmpRoleEventOutcome
import org.cangnova.cangjie.CjmpRoleSyntaxIdentity
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class CjmpDeclarationRoleParsingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun declarationChecksUseDirectKeywordOffsets() {
        val source = "class Owner { common func member(): Unit {} }\ncommon func top(): Unit {}"
        val offsets = listOf(source.indexOf("class"), source.indexOf("func member"), source.indexOf("func top"))
        fun facts(context: CjmpParsingContext): Map<Int, CjmpDeclarationRoleCheck> {
            val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
            return CangJieLightParser.parse(builder, cjmpContext = context).declarationRoleChecks
        }
        assertEquals(offsets.associateWith { CjmpDeclarationRoleCheck.NOT_CHECKED }, facts(CjmpParsingContext.SyntaxOnly))
        assertEquals(offsets.associateWith { CjmpDeclarationRoleCheck.NOT_CHECKED }, facts(CjmpParsingContext.Compilation(true, false, false)))
        assertEquals(mapOf(offsets[0] to CjmpDeclarationRoleCheck.NOT_CHECKED, offsets[1] to CjmpDeclarationRoleCheck.ACCEPTED,
            offsets[2] to CjmpDeclarationRoleCheck.ACCEPTED), facts(CjmpParsingContext.Compilation(true, false, true)))
        assertEquals(mapOf(offsets[0] to CjmpDeclarationRoleCheck.NOT_CHECKED, offsets[1] to CjmpDeclarationRoleCheck.REJECTED,
            offsets[2] to CjmpDeclarationRoleCheck.REJECTED), facts(CjmpParsingContext.Compilation(false, true, true)))
    }

    @Test
    fun roleEventsFollowCheckExecutionRatherThanDeclarationCompletion() {
        val source = "common class Owner { specific func member(): Unit {} }\ncommon func top(): Unit {}"
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val parsed = CangJieLightParser.parse(builder)
        assertEquals(listOf(source.indexOf("class"), source.indexOf("func member"), source.indexOf("func top")),
            parsed.roleEvents.map { it.declarationOffset })
        assertEquals(listOf(
            CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)),
            CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
            CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)),
        ), parsed.roleEvents.map { it.kind })
    }

    @Test
    fun historicalPlatformChangesRoleEventsAcrossParsingContexts() {
        val source = "platform func f(): Unit {}"
        fun events(context: CjmpParsingContext): List<CjmpParsedRoleEvent> {
            val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
            return CangJieLightParser.parse(builder, cjmpContext = context).roleEvents
        }
        val syntaxOnly = events(CjmpParsingContext.SyntaxOnly)
        val current = events(CjmpParsingContext.Compilation(true, false, true, platformModifierSyntaxAvailable = false))
        assertTrue(syntaxOnly.any { it.kind == CjmpRoleEventKind.Check(CjmpRoleModifiers(false, false, true)) })
        assertNotEquals(syntaxOnly, current)
    }

    @Test
    fun bufferedDiagnosticsBelongToTheirParseResult() {
        val source = "specific func wrong() {}"
        val context = CjmpParsingContext.Compilation(true, false, true)
        val psi = CjPsiFactory(project, markGenerated = false).createFile(source)
        psi.cjmpParsingContext = context
        val psiDiagnostics = psi.collectParsingDiagnostics()
        val sourceContext = checkNotNull(publishedParserSeed(psi).sourceContext)
        fun builder() = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val buffered = parseLightWithPsiInput(psi, builder())
        val delivered = mutableListOf<org.cangnova.cangjie.messages.ParsingDiagnostic>()
        // 额外保留真实 listener 入口。该 API 独立创建输入快照，分别验收 raw 与其本次 canonical 来源。
        val delivery = CangJieLightParser.parse(builder(), errorListener = { delivered.add(it) }, cjmpContext = context)
        assertTrue(buffered.diagnostics.isNotEmpty())
        val expected = listOf(ParsingDiagnostic(0, 0,
            CangJieParsingBundle.message("parsing.error.cjmp.specific.in.non.specific.file"),
            ParsingDiagnostic.Origin.ORIGINAL_FILE))
        assertLegacyParsingDiagnostics(expected, buffered.diagnostics, sourceContext)
        assertLegacyParsingDiagnostics(expected, delivered, delivery.sourceContext)
        assertPublishedParsingDiagnosticParity(delivery.diagnosticPublication.diagnostics, delivered, delivery.sourceContext)
        assertSame(delivery.sourceContext.snapshot, delivery.lexicalInput.source)
        assertEquals(source, delivery.sourceContext.snapshot.text)
        assertPublishedParsingDiagnosticParity(psiDiagnostics, buffered.diagnostics, sourceContext)
        assertEquals(CjmpFileRole.NONE, buffered.file.fileFacts.cjmpRole)
    }

    @Test
    fun lazyPsiBodyPublishesUnknownRoleRegion() {
        val source = "common func f(): Unit { specific func inner(): Unit {} }"
        val file = CjPsiFactory(project, markGenerated = false).createFile(source)
        requireNotNull(file.findElementAt(source.indexOf("func")))
        val events = requireNotNull(file.parsedRoleEvents)
        assertEquals(CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)), events.first().kind)
        assertTrue(events.any { it.kind == CjmpRoleEventKind.Unknown })
    }

    @Test
    fun lazyBodyPreservesUnknownBeforeFollowingFileRoleCheck() {
        val source = "common func first(): Unit { specific func nested(): Unit {} }\ncommon func last(): Unit {}"
        val file = CjPsiFactory(project, markGenerated = false).createFile(source)
        requireNotNull(file.findElementAt(source.indexOf("func first")))
        val beforeExpansion = requireNotNull(file.parsedRoleSnapshot)
        val events = beforeExpansion.events
        val first = events.indexOfFirst { it.declarationOffset == source.indexOf("func first") }
        val unknown = events.indexOfFirst { it.kind == CjmpRoleEventKind.Unknown }
        val last = events.indexOfFirst { it.declarationOffset == source.indexOf("func last") }
        assertTrue(first >= 0 && first < unknown && unknown < last, events.toString())
        val frozenEvents = events.toList()
        val nestedKeyword = requireNotNull(file.findElementAt(source.indexOf("func nested"))).node
        assertEquals(frozenEvents, beforeExpansion.events)
        // 局部惰性解析属于另一代事务，不能按相同文本或偏移认领文件快照事件。
        assertNull(nestedKeyword.cjmpDeclarationRoleEventsIn(beforeExpansion))
        val replay = CjmpRoleSnapshotReplay.replay(
            beforeExpansion, CjmpFileRoleMode.Compilation(true, true, false, false),
            LanguageVersionSettingsImpl.DEFAULT, CjmpRoleSyntaxIdentity.VERIFIED_EQUIVALENT,
        )
        val later = requireNotNull(replay.resultsFor(listOf(events[last]))).single()
        assertEquals(CjmpRoleEventOutcome.UNKNOWN, later.outcome)
    }

    @Test
    fun nonRoleModifiersStillTriggerAFileRoleCheck() {
        val source = "class Owner { func plain(): Unit {}\npublic func visible(): Unit {}\ninternal func internalVisible(): Unit {} }"
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val facts = CangJieLightParser.parse(builder, cjmpContext = CjmpParsingContext.Compilation(true, false, true))
            .declarationRoleChecks
        assertEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, facts[source.indexOf("class")])
        assertEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, facts[source.indexOf("func plain")])
        assertEquals(CjmpDeclarationRoleCheck.ACCEPTED, facts[source.indexOf("func visible")])
        assertEquals(CjmpDeclarationRoleCheck.ACCEPTED, facts[source.indexOf("func internalVisible")])
    }

    @Test
    fun invalidMultipleExtendModifiersDoNotChangeFileRole() {
        val source = "public specific extend Int64 {}\ncommon func next(): Unit {}"
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val parsed = CangJieLightParser.parse(builder, cjmpContext = CjmpParsingContext.Compilation(true, true, true))
        assertEquals(CjmpFileRole.COMMON, parsed.fileFacts.cjmpRole)
        assertEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.declarationRoleChecks[source.indexOf("func")])
    }

    @Test
    fun singleExtendRoleStillParticipatesInFileRoleChecking() {
        val source = "specific extend Int64 {}\ncommon func next(): Unit {}"
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val parsed = CangJieLightParser.parse(builder, cjmpContext = CjmpParsingContext.Compilation(true, true, true))
        assertEquals(CjmpFileRole.SPECIFIC, parsed.fileFacts.cjmpRole)
        assertEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.declarationRoleChecks[source.indexOf("extend")])
        assertEquals(CjmpDeclarationRoleCheck.REJECTED, parsed.declarationRoleChecks[source.indexOf("func")])
    }

    @Test
    fun extendModifierSetUsesOriginalTokenIdentity() {
        fun role(source: String, context: CjmpParsingContext.Compilation): CjmpFileRole {
            val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
            return CangJieLightParser.parse(builder, cjmpContext = context).fileFacts.cjmpRole
        }
        val context = CjmpParsingContext.Compilation(true, true, true, platformModifierSyntaxAvailable = true)
        assertEquals(CjmpFileRole.COMMON, role("platform specific extend Int64 {}\ncommon func next(): Unit {}", context))
        assertEquals(CjmpFileRole.SPECIFIC, role("specific specific extend Int64 {}\ncommon func next(): Unit {}", context))
    }

    @Test
    fun rejectedExtendDoesNotChangeTheFileRole() {
        val source = "specific extend Int64 {}\ncommon func next(): Unit {}"
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val parsed = CangJieLightParser.parse(builder, cjmpContext = CjmpParsingContext.Compilation(true, false, true))
        assertEquals(CjmpFileRole.COMMON, parsed.fileFacts.cjmpRole)
        assertEquals(CjmpDeclarationRoleCheck.REJECTED, parsed.declarationRoleChecks[source.indexOf("extend")])
        assertEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.declarationRoleChecks[source.indexOf("func")])
    }

    @Test
    fun psiTokensCarryRoleFactsFromTheSameParse() {
        val source = "common class Owner { common func member(): Unit {} }"
        val file = CjPsiFactory(project, markGenerated = false).createFile(source)
        file.cjmpParsingContext = CjmpParsingContext.Compilation(false, true, true)
        for (offset in listOf(source.indexOf("class"), source.indexOf("func"))) {
            val token = requireNotNull(file.findElementAt(offset))
            assertEquals(CjmpDeclarationRoleCheck.REJECTED, token.node.cjmpDeclarationRoleCheckOrNull())
        }
    }
}
