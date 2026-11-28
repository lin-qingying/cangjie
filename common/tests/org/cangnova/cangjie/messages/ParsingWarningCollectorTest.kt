package org.cangnova.cangjie.messages

import org.cangnova.cangjie.parsing.CjModifierKind
import org.cangnova.cangjie.parsing.CjParserSourceContext
import org.cangnova.cangjie.parsing.CjParsingSourceSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** 固定 modifier-warning-authority/29 的报告事实，以及原生 suppression/dedup/LEX 发布契约。 */
class ParsingWarningCollectorTest {
    @Test
    fun modifierWarningsRetainNativeIdentityRangesHintsAndNotes() {
        val context = context(CLASS_SOURCE)
        val collector = CjParsingDiagnosticCollector(context)
        val publicWarning = classWarning(CjModifierKind.PUBLIC, 1, 7)
        val openWarning = classWarning(CjModifierKind.OPEN, 8, 12)
        collector.report(publicWarning)
        collector.report(openWarning)

        val result = collector.finish()

        assertEquals(listOf(publicWarning.message, openWarning.message), result.map { it.message })
        assertEquals(listOf(3 to 1, 3 to 8), result.map { assertNotNull(it.mainRange).begin })
        assertEquals(listOf(3 to 7, 3 to 12), result.map { assertNotNull(it.mainRange).end })
        result.zip(listOf(publicWarning, openWarning)).forEach { (actual, expected) ->
            assertEquals(ParsingDiagnostic.Severity.WARNING, actual.severity)
            assertFalse(actual.isError)
            assertEquals(ParsingDiagnostic.Category.PARSE, actual.category)
            assertEquals(expected.warning, actual.warning)
            assertEquals("parse_redundant_modifier", assertNotNull(actual.warning).nativeKind)
            assertEquals(WarningGroup.PARSER, actual.warning?.group)
            assertEquals(expected.mainHint, actual.mainHint)
            assertEquals(listOf(SUPPRESSION_NOTE), actual.notes)
            assertEquals(expected.hints, actual.hints)
            assertSame(context.snapshot, assertNotNull(actual.mainRange).source)
            assertEquals(listOf(CjCanonicalParsingDiagnosticHint(
                CjParsingDiagnosticRange(context.snapshot, 3 to 22, 3 to 28), null,
            )), actual.nativeHints)
        }
        assertFalse(result.any { it.isError })
        assertEquals(result, collector.publication.diagnostics)
    }

    /** 两种严重级别不能在完整范围去重中互相抢占；同级重复仍保留首报的事实。 */
    @Test
    fun warningAndErrorAtSameRangeRemainSeparateWhileDuplicateWarningKeepsFirstDetails() {
        val collector = CjParsingDiagnosticCollector(context(CLASS_SOURCE))
        val warning = classWarning(CjModifierKind.OPEN, 8, 12)
        val error = ParsingDiagnostic(warning.startOffset, warning.endOffset, "expected expression")
        collector.report(warning)
        collector.report(warning.copy(mainHint = "later report", notes = emptyList(), hints = emptyList()))
        collector.report(error)

        val result = collector.finish()

        assertEquals(listOf(ParsingDiagnostic.Severity.WARNING, ParsingDiagnostic.Severity.ERROR), result.map { it.severity })
        assertEquals(warning.warning, result[0].warning)
        assertEquals(warning.mainHint, result[0].mainHint)
        assertEquals(warning.notes, result[0].notes)
        assertEquals(warning.hints, result[0].hints)
        assertEquals(error.message, result[1].message)
        assertNull(result[1].warning)
    }

    /** 独立报告事件故意共享文本以观察发布契约，不声明 parser 会将 redundant warning 当 ERROR 产生。 */
    @Test
    fun suppressionPrecedesFirstMessageDedupAndCannotHideAnUnsuppressedError() {
        val warning = classWarning(CjModifierKind.OPEN, 8, 12)
        val error = ParsingDiagnostic(warning.startOffset, warning.endOffset, warning.message)
        for (settings in listOf(
            ParsingDiagnosticSettings(disableWarnings = true),
            ParsingDiagnosticSettings(suppressedGroups = setOf(WarningGroup.PARSER)),
        )) {
            val collector = CjParsingDiagnosticCollector(context(CLASS_SOURCE), settings)
            collector.report(warning)
            collector.report(error)

            val result = collector.finish()

            assertEquals(1, result.size)
            assertEquals(ParsingDiagnostic.Severity.ERROR, result.single().severity)
            assertEquals(error.message, result.single().message)
            assertNull(result.single().warning)
            assertEquals(2, collector.collectedReports.size, "保存报告不能因 suppression 丢失 producer 事件")
        }
    }

    @Test
    fun parserGroupSuppressionDoesNotSuppressParserErrors() {
        val context = context(PARSE_ERROR_SOURCE)
        val collector = CjParsingDiagnosticCollector(context,
            ParsingDiagnosticSettings(suppressedGroups = setOf(WarningGroup.PARSER)))
        collector.report(interfaceWarning(PARSE_ERROR_SOURCE))
        val error = illegalStructError(PARSE_ERROR_SOURCE)
        collector.report(error)

        val result = collector.finish()

        assertEquals(listOf(error.message), result.map { it.message })
        assertTrue(result.single().isError)
        assertEquals(ParsingDiagnostic.Category.PARSE, result.single().category)
    }

    /** 同 category 的 Parse ERROR 不会撤销已经保存的合法 WARNING。 */
    @Test
    fun parserErrorLeavesModifierWarningInPublication() {
        val collector = CjParsingDiagnosticCollector(context(PARSE_ERROR_SOURCE))
        val warning = interfaceWarning(PARSE_ERROR_SOURCE)
        val error = illegalStructError(PARSE_ERROR_SOURCE)
        collector.report(warning)
        collector.report(error)

        val result = collector.finish()

        assertEquals(listOf(warning.message, error.message), result.map { it.message })
        assertEquals(listOf(ParsingDiagnostic.Severity.WARNING, ParsingDiagnostic.Severity.ERROR), result.map { it.severity })
        assertEquals(warning.warning, result[0].warning)
    }

    /** SDK lex-unit-gate：另一源根的 LEX ERROR 关闭整个编译单元的 PARSE 输出。 */
    @Test
    fun lexicalErrorInAnotherRootSuppressesAllParserReportsButKeepsTheirInputFacts() {
        val warningSource = "package modifier_lex_unit_gate\n\nopen interface I {}\n"
        val lexSource = "package modifier_lex_unit_gate\n\n\\\n"
        val warningRoot = CjParsingDiagnosticCollector(context(warningSource, "warning.cj"))
        warningRoot.report(interfaceWarning(warningSource))
        assertEquals(1, warningRoot.finish().size)
        val lexRoot = CjParsingDiagnosticCollector(context(lexSource, "lex.cj"))
        val start = offset(lexSource, 3, 1)
        lexRoot.report(ParsingDiagnostic(start, start, "unknown start of token: \\",
            category = ParsingDiagnostic.Category.LEX, rangeKind = ParsingDiagnostic.RangeKind.POINT))
        lexRoot.finish()

        val result = CjParsingDiagnosticCollector.publishSyntaxDiagnostics(warningRoot.collectedReports + lexRoot.collectedReports)

        assertEquals(1, result.size)
        assertEquals(ParsingDiagnostic.Category.LEX, result.single().category)
        assertTrue(result.single().isError)
        assertEquals(3 to 1, result.single().mainRange?.begin)
        assertEquals(3 to 2, result.single().mainRange?.end)
        assertEquals(ParsingDiagnostic.Severity.WARNING, warningRoot.collectedReports.single().diagnostic.severity)
        assertEquals(1, warningRoot.publication.diagnostics.size)
    }

    /** 重放完整已保存事件时由本次发布策略决定 suppression，不将被抑制报告从输入图抹掉。 */
    @Test
    fun savedWarningReportsCanBePublishedWithTheConsumersPolicy() {
        val collector = CjParsingDiagnosticCollector(context(CLASS_SOURCE), ParsingDiagnosticSettings(disableWarnings = true))
        collector.report(classWarning(CjModifierKind.OPEN, 8, 12))
        assertTrue(collector.finish().isEmpty())
        assertEquals(1, collector.collectedReports.size)

        val enabled = CjParsingDiagnosticCollector.publishSyntaxDiagnostics(collector.collectedReports)
        val disabled = CjParsingDiagnosticCollector.publishSyntaxDiagnostics(collector.collectedReports,
            ParsingDiagnosticSettings(suppressedGroups = setOf(WarningGroup.PARSER)))

        assertEquals(1, enabled.size)
        assertEquals(ParsingDiagnostic.Severity.WARNING, enabled.single().severity)
        assertTrue(disabled.isEmpty())
    }

    @Test
    fun settingsRetainTheirSuppressedGroupSnapshot() {
        val groups = mutableSetOf(WarningGroup.PARSER)
        val settings = ParsingDiagnosticSettings(suppressedGroups = groups)
        groups.clear()
        val collector = CjParsingDiagnosticCollector(context(CLASS_SOURCE), settings)
        collector.report(classWarning(CjModifierKind.OPEN, 8, 12))

        assertTrue(collector.finish().isEmpty())
        assertEquals(setOf(WarningGroup.PARSER), settings.suppressedGroups)
    }

    private fun classWarning(modifier: CjModifierKind, begin: Int, end: Int): ParsingDiagnostic {
        val name = if (modifier == CjModifierKind.PUBLIC) "public" else "open"
        return ParsingDiagnostic(offset(CLASS_SOURCE, 3, begin), offset(CLASS_SOURCE, 3, end), "redundant modifier: '$name'",
            severity = ParsingDiagnostic.Severity.WARNING,
            warning = ParsingWarning.RedundantModifier(modifier, ParsingWarning.Redundancy.Explicit(CjModifierKind.SEALED)),
            mainHint = "'sealed' implies '$name'", notes = listOf(SUPPRESSION_NOTE),
            hints = listOf(ParsingDiagnosticHint(offset(CLASS_SOURCE, 3, 22), offset(CLASS_SOURCE, 3, 28))))
    }

    private fun interfaceWarning(text: String): ParsingDiagnostic = ParsingDiagnostic(
        offset(text, 3, 1), offset(text, 3, 5), "redundant modifier: 'open'",
        severity = ParsingDiagnostic.Severity.WARNING,
        warning = ParsingWarning.RedundantModifier(CjModifierKind.OPEN, ParsingWarning.Redundancy.InterfaceMember),
        mainHint = "interface member implies 'open'", notes = listOf(SUPPRESSION_NOTE),
        hints = listOf(ParsingDiagnosticHint(offset(text, 3, 6), offset(text, 3, 15))),
    )

    private fun illegalStructError(text: String): ParsingDiagnostic = ParsingDiagnostic(
        offset(text, 4, 1), offset(text, 4, 5), "unexpected modifier 'open' on struct declaration",
        mainHint = "unexpected modifier",
        hints = listOf(ParsingDiagnosticHint(offset(text, 4, 6), offset(text, 4, 12), "on struct declaration")),
    )

    private fun context(text: String, name: String = "warning.cj"): CjParserSourceContext =
        CjParserSourceContext(CjParsingSourceSnapshot(name, name, text), 0)

    /** 固定 SDK ASCII 行列到输入偏移的换算，不使用被测 collector 的定位算法生成期望。 */
    private fun offset(text: String, line: Int, column: Int): Int =
        text.lines().take(line - 1).sumOf { it.length + 1 } + column - 1

    private companion object {
        const val SUPPRESSION_NOTE = "this warning can be suppressed by setting the compiler option `-Woff parser`"
        const val CLASS_SOURCE = "package modifier_class_two_warnings_priority\n\npublic open abstract sealed class C {}\n"
        const val PARSE_ERROR_SOURCE = "package modifier_parse_error_keeps_warning\n\nopen interface I {}\nopen struct S {}\n"
    }
}
