package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.messages.CjCanonicalParsingDiagnosticHint
import org.cangnova.cangjie.messages.CjParsingDiagnosticCollector
import org.cangnova.cangjie.messages.CjParsingDiagnosticRange
import org.cangnova.cangjie.messages.CjParsingDiagnosticOrder
import org.cangnova.cangjie.messages.CjParsingDiagnosticReport
import org.cangnova.cangjie.messages.CjParsingDiagnosticScope
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnostic.Category.LEX
import org.cangnova.cangjie.messages.ParsingDiagnostic.Category.PARSE
import org.cangnova.cangjie.messages.ParsingDiagnostic.Origin.INPUT
import org.cangnova.cangjie.messages.ParsingDiagnostic.Origin.ORIGINAL_FILE
import org.cangnova.cangjie.messages.ParsingDiagnostic.RangeKind.POINT
import org.cangnova.cangjie.messages.ParsingDiagnostic.RangeKind.RANGE
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import java.util.UUID

/**
 * 纯模型回归：只构造来源快照和诊断 DTO，不创建 PSI、parser 或 session。
 * 官方 v1.1.3 DiagnosticEngine 按类别、完整主范围、severity 保存首次，再按原生主范围发射。
 */
class CjParsingDiagnosticCollectorTest {
    @Test
    fun duplicateMessagesAndHintsKeepTheFirstReportAndFinishClosesReporting() {
        val context = CjParserSourceContext.complete("abcd")
        val collector = CjParsingDiagnosticCollector(context)
        val producerHints = mutableListOf(ParsingDiagnosticHint(3, 3, "insert here", rangeKind = POINT))
        val first = ParsingDiagnostic(
            1, 2, "expected identifier",
            hints = producerHints,
        )
        val originalFirst = first.copy(hints = producerHints.toList())
        val duplicate = first.copy(
            message = "expected expression",
            hints = listOf(ParsingDiagnosticHint(0, 1, "another hint")),
        )
        // ASCII 的 RANGE [1:2,1:3] 与 POINT [1:2,1:3] 相同；rangeKind 也不是重复键。
        val sameNativePoint = duplicate.copy(startOffset = 1, endOffset = 1, rangeKind = POINT)
        val earlier = ParsingDiagnostic(0, 1, "earlier main range")
        assertEquals(PARSE, first.category)
        assertEquals(RANGE, first.rangeKind)

        collector.report(first)
        // producer 随后修改自己的提示列表，不得改写首次保存的原始提示或规范提示。
        producerHints.clear()
        collector.report(duplicate)
        collector.report(sameNativePoint)
        collector.report(earlier)
        val result = collector.finish()
        assertEquals(listOf(earlier.message, first.message), result.map { it.message })
        assertOriginalFields(earlier, result[0])
        assertOriginalFields(originalFirst, result[1])
        assertEquals(CjParsingDiagnosticRange(context.snapshot, 1 to 1, 1 to 2), assertNotNull(result[0].mainRange))
        assertEquals(CjParsingDiagnosticRange(context.snapshot, 1 to 2, 1 to 3), assertNotNull(result[1].mainRange))
        assertEquals(emptyList(), result[0].nativeHints)
        assertEquals(listOf(CjCanonicalParsingDiagnosticHint(
            CjParsingDiagnosticRange(context.snapshot, 1 to 4, 1 to 5), "insert here",
        )), result[1].nativeHints)
        assertNull(first.mainRange)
        assertEquals(emptyList(), first.nativeHints)

        // finish 后连已存在的重复键也必须拒绝，不能先去重再跳过状态检查。
        assertFailsWith<IllegalStateException> { collector.report(first) }
        assertFailsWith<IllegalStateException> { collector.report(ParsingDiagnostic(2, 3, "late report")) }
    }

    @Test
    fun lexicalCategorySurvivesASameRangeParserReportAndSuppressesAllParseReports() {
        val context = CjParserSourceContext.complete("§a§b")
        val collector = CjParsingDiagnosticCollector(context)
        val parseAtLexRange = ParsingDiagnostic(0, 0, "expected expression", rangeKind = POINT)
        val laterLex = ParsingDiagnostic(2, 2, "unknown token start later", category = LEX, rangeKind = POINT)
        val earlierLex = ParsingDiagnostic(
            0, 0, "unknown token start", category = LEX, rangeKind = POINT,
            hints = listOf(ParsingDiagnosticHint(0, 0, "invalid start", rangeKind = POINT)),
        )
        collector.report(parseAtLexRange)
        collector.report(laterLex)
        collector.report(ParsingDiagnostic(1, 2, "expected closing delimiter"))
        collector.report(earlierLex)
        collector.report(earlierLex.copy(
            message = "different lexical message",
            hints = listOf(ParsingDiagnosticHint(4, 4, "at EOF", rangeKind = POINT)),
        ))
        collector.report(ParsingDiagnostic(3, 4, "parser report after lexical reports"))

        // PARSE 不能抢占同范围的 LEX；LEX 门作用于全部 PARSE，而非仅重叠的范围。
        val result = collector.finish()
        assertEquals(listOf(earlierLex.message, laterLex.message), result.map { it.message })
        assertOriginalFields(earlierLex, result[0])
        assertOriginalFields(laterLex, result[1])
        // UNKNOWN_TOKEN_START 是 POINT；§ 的实际 UTF-8 宽度不能成为主范围宽度。
        assertEquals(CjParsingDiagnosticRange(context.snapshot, 1 to 1, 1 to 2), assertNotNull(result[0].mainRange))
        assertEquals(CjParsingDiagnosticRange(context.snapshot, 1 to 4, 1 to 5), assertNotNull(result[1].mainRange))
        assertEquals(listOf(CjCanonicalParsingDiagnosticHint(
            CjParsingDiagnosticRange(context.snapshot, 1 to 1, 1 to 2), "invalid start",
        )), result[0].nativeHints)
        assertEquals(emptyList(), result[1].nativeHints)
    }

    @Test
    fun completeNativeRangesSortAcrossUtf8AndCrLfSourcesWithoutMergingDifferentEnds() {
        val utf8 = CjParsingSourceSnapshot("utf8.cj", null, "é\r\n中x")
        val ascii = CjParsingSourceSnapshot("ascii.cj", null, "abcdeX\r\nQ")
        val collector = CjParsingDiagnosticCollector(CjParserSourceContext(utf8, 0))
        val short = ParsingDiagnostic(0, 1, "same message", source = utf8)
        val acrossCrLf = short.copy(endOffset = 3, message = "same start with another end")
        val asciiFirstLine = ParsingDiagnostic(
            5, 6, "first line at a larger offset", source = ascii,
            hints = listOf(
                ParsingDiagnosticHint(0, 3, "spans CRLF", source = utf8),
                ParsingDiagnosticHint(2, 2, "point at LF", source = utf8, rangeKind = POINT),
            ),
        )
        val utf8SecondLine = ParsingDiagnostic(3, 4, "second line at a smaller offset", source = utf8)
        assertEquals(1 to 3, utf8.nativePosition(1))
        assertEquals(2 to 1, utf8.nativePosition(3))
        assertEquals(2 to 4, utf8.nativePosition(4))
        assertEquals(1 to 6, ascii.nativePosition(5))

        collector.report(utf8SecondLine)
        collector.report(asciiFirstLine)
        collector.report(acrossCrLf)
        collector.report(short)

        // 同起点先比较完整终点；跨来源按原生行列排序，不能用 DTO 的 UTF-16 偏移排序。
        val expected = listOf(short, acrossCrLf, asciiFirstLine, utf8SecondLine)
        val result = collector.finish()
        assertEquals(expected.map { it.message }, result.map { it.message })
        expected.zip(result).forEach { (original, completed) ->
            assertOriginalFields(original, completed)
        }
        assertEquals(listOf(
            CjParsingDiagnosticRange(utf8, 1 to 1, 1 to 3),
            CjParsingDiagnosticRange(utf8, 1 to 1, 2 to 1),
            CjParsingDiagnosticRange(ascii, 1 to 6, 1 to 7),
            CjParsingDiagnosticRange(utf8, 2 to 1, 2 to 4),
        ), result.map { assertNotNull(it.mainRange) })
        assertEquals(listOf(
            emptyList(),
            emptyList(),
            listOf(
                CjCanonicalParsingDiagnosticHint(CjParsingDiagnosticRange(utf8, 1 to 1, 2 to 1), "spans CRLF"),
                CjCanonicalParsingDiagnosticHint(CjParsingDiagnosticRange(utf8, 1 to 4, 1 to 5), "point at LF"),
            ),
            emptyList(),
        ), result.map { it.nativeHints })
    }

    @Test
    fun pointsAtMultibyteNewlineAndEofPositionsDoNotCollapseIntoRealRanges() {
        val context = CjParserSourceContext.complete("é\r\n中\r\n")
        val collector = CjParsingDiagnosticCollector(context)
        val multibytePoint = ParsingDiagnostic(0, 0, "multibyte point", rangeKind = POINT)
        val multibyteRange = ParsingDiagnostic(0, 1, "full multibyte character")
        val newlinePoint = ParsingDiagnostic(2, 2, "point at LF", rangeKind = POINT)
        val acrossNewline = ParsingDiagnostic(2, 3, "range ending on the next line")
        val eofRange = ParsingDiagnostic(6, 6, "empty real EOF range")
        val eofPoint = ParsingDiagnostic(6, 6, "one native column at EOF", rangeKind = POINT)
        assertEquals(1 to 4, context.snapshot.nativePosition(2))
        assertEquals(2 to 1, context.snapshot.nativePosition(3))
        assertEquals(3 to 1, context.snapshot.nativePosition(6))

        val expected = listOf(multibytePoint, multibyteRange, newlinePoint, acrossNewline, eofRange, eofPoint)
        expected.asReversed().forEach(collector::report)

        // POINT 终点只加一列：不跨越换行、不按 UTF-8 字符宽度扩张，也不能在 EOF 被裁成零宽。
        val result = collector.finish()
        assertEquals(expected.map { it.message }, result.map { it.message })
        expected.zip(result).forEach { (original, completed) ->
            assertOriginalFields(original, completed)
            assertEquals(emptyList(), completed.nativeHints)
        }
        assertEquals(listOf(
            CjParsingDiagnosticRange(context.snapshot, 1 to 1, 1 to 2),
            CjParsingDiagnosticRange(context.snapshot, 1 to 1, 1 to 3),
            CjParsingDiagnosticRange(context.snapshot, 1 to 4, 1 to 5),
            CjParsingDiagnosticRange(context.snapshot, 1 to 4, 2 to 1),
            CjParsingDiagnosticRange(context.snapshot, 3 to 1, 3 to 1),
            CjParsingDiagnosticRange(context.snapshot, 3 to 1, 3 to 2),
        ), result.map { assertNotNull(it.mainRange) })
    }

    @Test
    fun originalFileDiagnosticsInAnEmptyInputShareTheNativeOriginPoint() {
        val context = CjParserSourceContext.complete("")
        val collector = CjParsingDiagnosticCollector(context)
        val originalPoint = ParsingDiagnostic(
            0, 0, "original file error", origin = ORIGINAL_FILE, rangeKind = POINT,
            hints = listOf(ParsingDiagnosticHint(0, 0, "file origin", rangeKind = POINT)),
        )
        collector.report(originalPoint)
        collector.report(originalPoint.copy(message = "different original file error", rangeKind = RANGE, hints = emptyList()))
        collector.report(originalPoint.copy(message = "input point at the same native range", origin = INPUT))
        val emptyInputRange = ParsingDiagnostic(0, 0, "real empty input range")
        collector.report(emptyInputRange)

        // ORIGINAL_FILE 无显式 source 时统一为 [1:1,1:2]；DTO 仍保留真实的零偏移点位。
        val result = collector.finish()
        assertEquals(listOf(emptyInputRange.message, originalPoint.message), result.map { it.message })
        assertOriginalFields(emptyInputRange, result[0])
        assertOriginalFields(originalPoint, result[1])
        assertEquals(CjParsingDiagnosticRange(context.snapshot, 1 to 1, 1 to 1), assertNotNull(result[0].mainRange))
        assertEquals(CjParsingDiagnosticRange(context.snapshot, 1 to 1, 1 to 2), assertNotNull(result[1].mainRange))
        assertEquals(emptyList(), result[0].nativeHints)
        assertEquals(listOf(CjCanonicalParsingDiagnosticHint(
            CjParsingDiagnosticRange(context.snapshot, 1 to 1, 1 to 2), "file origin",
        )), result[1].nativeHints)

        val emptyCollector = CjParsingDiagnosticCollector(context)
        assertEquals(emptyList(), emptyCollector.finish())
        assertFailsWith<IllegalStateException> { emptyCollector.report(originalPoint) }
    }

    @Test
    fun independentSnapshotsStayDistinctWhileSliceAndExplicitSourceAliasesDeduplicate() {
        val snapshot = CjParsingSourceSnapshot("same.cj", "/same.cj", "xé\r\nz")
        val otherSnapshot = CjParsingSourceSnapshot("same.cj", "/same.cj", "xé\r\nz")
        val collector = CjParsingDiagnosticCollector(CjParserSourceContext(snapshot, inputStart = 1, inputEnd = 2))
        val sliceReport = ParsingDiagnostic(
            0, 1, "first report from the input slice",
            hints = listOf(ParsingDiagnosticHint(0, 0, "slice hint", rangeKind = POINT)),
        )
        val explicitAlias = ParsingDiagnostic(
            1, 2, "later report from the full snapshot", source = snapshot,
            hints = listOf(ParsingDiagnosticHint(4, 5, "full snapshot hint", source = snapshot)),
        )
        val independent = explicitAlias.copy(message = "same coordinates in another snapshot", source = otherSnapshot)
        assertNotEquals(snapshot.identity, otherSnapshot.identity)
        assertEquals(1 to 2, snapshot.nativePosition(1))
        assertEquals(1 to 4, snapshot.nativePosition(2))

        collector.report(sliceReport)
        collector.report(explicitAlias)
        collector.report(independent)
        val result = collector.finish()

        // 相同路径、文本和原生坐标不等于同一快照；相同范围的跨快照排序不额外限定先后。
        assertEquals(2, result.size)
        assertEquals(setOf(sliceReport.message, independent.message), result.map { it.message }.toSet())
        val completedSlice = result.single { it.message == sliceReport.message }
        val completedIndependent = result.single { it.message == independent.message }
        assertOriginalFields(sliceReport, completedSlice)
        assertOriginalFields(independent, completedIndependent)
        assertEquals(CjParsingDiagnosticRange(snapshot, 1 to 2, 1 to 4), assertNotNull(completedSlice.mainRange))
        assertEquals(CjParsingDiagnosticRange(otherSnapshot, 1 to 2, 1 to 4), assertNotNull(completedIndependent.mainRange))
        assertEquals(listOf(CjCanonicalParsingDiagnosticHint(
            CjParsingDiagnosticRange(snapshot, 1 to 2, 1 to 3), "slice hint",
        )), completedSlice.nativeHints)
        // 主诊断属于另一快照，显式提示仍属于它自己声明的来源，不借用主范围 source。
        assertEquals(listOf(CjCanonicalParsingDiagnosticHint(
            CjParsingDiagnosticRange(snapshot, 2 to 1, 2 to 2), "full snapshot hint",
        )), completedIndependent.nativeHints)
    }

    @Test
    fun sameSourceBeginAndMessageKeepTheProducerFirstRangeDespiteDifferentEnds() {
        val context = CjParserSourceContext.complete("abcd")
        val scope = CjParsingDiagnosticScope(UUID.randomUUID())
        val first = ParsingDiagnostic(1, 3, "same message", hints = listOf(ParsingDiagnosticHint(3, 4, "first hint")))
        val later = first.copy(endOffset = 2, hints = listOf(ParsingDiagnosticHint(0, 1, "later hint")))
        val collector = CjParsingDiagnosticCollector(context)
        // 较短范围的后报先被树遍历看到；不能先按范围排序再执行官方 prevDiags。
        collector.restoreReport(CjParsingDiagnosticReport(later, CjParsingDiagnosticOrder(scope, 1)))
        collector.restoreReport(CjParsingDiagnosticReport(first, CjParsingDiagnosticOrder(scope, 0)))
        val result = collector.finish().single()
        assertOriginalFields(first, result)
        assertEquals(CjParsingDiagnosticRange(context.snapshot, 1 to 2, 1 to 4), result.mainRange)
        assertEquals(listOf("first hint"), result.nativeHints.map { it.label })
        assertEquals(listOf(first.message, later.message), collector.collectedReports.map { it.diagnostic.message })
        assertFailsWith<IllegalStateException> {
            collector.restoreReport(CjParsingDiagnosticReport(first, CjParsingDiagnosticOrder(scope, 2)))
        }
    }

    @Test
    fun categoryRangeRejectionStillContributesToTheCompilationUnitsPreviousReportCache() {
        val context = CjParserSourceContext.complete("abcd")
        val firstRoot = CjParsingDiagnosticCollector(context)
        val first = ParsingDiagnostic(1, 2, "range winner")
        val rejected = first.copy(message = "previous message")
        firstRoot.report(first)
        firstRoot.report(rejected)
        assertEquals(listOf(first.message), firstRoot.finish().map { it.message })
        assertEquals(2, firstRoot.publication.reports.size)

        val nextRoot = CjParsingDiagnosticCollector(context)
        nextRoot.report(rejected.copy(endOffset = 3))
        assertEquals(listOf(rejected.message), nextRoot.finish().map { it.message })
        // 第一根的第二报告虽被 category 范围去重，仍占据同来源/起点/消息的 prevDiags。
        val published = CjParsingDiagnosticCollector.publishSyntaxDiagnostics(
            firstRoot.publication.reports + nextRoot.publication.reports,
        )
        assertEquals(listOf(first.message), published.map { it.message })
        assertOriginalFields(first, published.single())
    }

    @Test
    fun lexicalReportViewsRetainProducerIdentityAndBindTheSameNativeSourceRange() {
        val source = CjParsingSourceSnapshot("input.cj", null, "x\r\n§")
        val input = CjLexicalSourceInput(source)
        val token = input.tokenAt(3)
        val whole = CjParserSourceContext(source, 0)
        val slice = CjParserSourceContext(source, 3, 4)
        val fullReport = assertNotNull(input.diagnosticForToken(token.id, whole))
        val sliceReport = assertNotNull(input.diagnosticForToken(token.id, slice))
        assertEquals(fullReport.order, sliceReport.order)
        assertEquals(3, fullReport.diagnostic.startOffset)
        assertEquals(0, sliceReport.diagnostic.startOffset)
        assertNull(fullReport.diagnostic.source)
        assertNull(sliceReport.diagnostic.source)

        for ((context, report) in listOf(whole to fullReport, slice to sliceReport)) {
            val collector = CjParsingDiagnosticCollector(context)
            collector.restoreReport(report)
            val published = collector.finish().single()
            assertEquals(LEX, published.category)
            assertEquals(POINT, published.rangeKind)
            assertEquals(CjParsingDiagnosticRange(source, 2 to 1, 2 to 2), published.mainRange)
            assertEquals(emptyList(), published.nativeHints)
        }
        assertFailsWith<IllegalArgumentException> {
            input.diagnosticForToken(token.id, CjParserSourceContext(source, 0, 1))
        }
    }

    /** 完成 DTO 增加规范位置，但原始坐标、来源、类别及首次提示必须原样保留。 */
    private fun assertOriginalFields(original: ParsingDiagnostic, completed: ParsingDiagnostic) {
        assertEquals(original.message, completed.message)
        assertEquals(original.startOffset, completed.startOffset)
        assertEquals(original.endOffset, completed.endOffset)
        assertSame(original.source, completed.source)
        assertEquals(original.origin, completed.origin)
        assertEquals(original.category, completed.category)
        assertEquals(original.rangeKind, completed.rangeKind)
        assertEquals(original.hints, completed.hints)
        assertNotNull(completed.mainRange)
    }
}
