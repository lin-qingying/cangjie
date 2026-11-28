package org.cangnova.cangjie.messages

import org.cangnova.cangjie.parsing.CjParserSourceContext
import org.cangnova.cangjie.parsing.CjParsingSourceSnapshot
import java.util.Collections
import java.util.UUID

/**
 * 官方语法诊断的共享发布 owner。直接报告按调用时间入队，树上的报告必须携带
 * producer 已分配的身份重放；完成时先按报告顺序去重，再按原生范围排序并应用 LEX 门。
 */
class CjParsingDiagnosticCollector(
    private val sourceContext: CjParserSourceContext,
    private val settings: ParsingDiagnosticSettings = ParsingDiagnosticSettings(),
) {
    private data class Key(
        val category: ParsingDiagnostic.Category,
        val sourceIdentity: UUID,
        val begin: Pair<Int, Int>,
        val end: Pair<Int, Int>,
        val severity: ParsingDiagnostic.Severity,
    )

    /** v1.1.3 prevDiags 在 category 集合之前过滤同来源、同起点、同消息。 */
    private data class PreviousKey(val sourceIdentity: UUID, val begin: Pair<Int, Int>, val message: String)

    private val directScope = CjParsingDiagnosticScope(UUID.randomUUID())
    private var nextDirectSequence = 0L
    private val captured = linkedMapOf<CjParsingDiagnosticOrder, CjParsingDiagnosticReport>()
    private var finished = false
    private var completedReports: List<CjParsingDiagnosticReport> = emptyList()
    private var completed: List<ParsingDiagnostic> = emptyList()
    private var published: List<ParsingDiagnostic> = emptyList()

    /** 已经处于真实 producer 调用顺序的直接报告；不用于 PSI/LT visitor。 */
    fun report(diagnostic: ParsingDiagnostic) {
        check(!finished) { "A completed parser diagnostic root cannot accept new reports." }
        restoreReport(CjParsingDiagnosticReport(diagnostic, CjParsingDiagnosticOrder(directScope, nextDirectSequence++)))
    }

    /** 重放已提交的树报告；顺序来自 producer，遍历只读取其不可变身份。 */
    fun restoreReport(report: CjParsingDiagnosticReport) {
        check(!finished) { "A completed parser diagnostic root cannot accept new reports." }
        val diagnostic = report.diagnostic
        val range = when {
            diagnostic.source != null -> resolve(diagnostic.source, diagnostic.startOffset, diagnostic.endOffset, diagnostic.rangeKind)
            diagnostic.origin == ParsingDiagnostic.Origin.ORIGINAL_FILE -> CjParsingDiagnosticRange(
                sourceContext.snapshot, 1 to 1, 1 to 2,
                org.cangnova.cangjie.parsing.CjParsingSourceRange(sourceContext.snapshot, 0, 0),
            )
            else -> resolve(sourceContext.snapshot, sourceContext.inputStart + diagnostic.startOffset,
                sourceContext.inputStart + diagnostic.endOffset, diagnostic.rangeKind)
        }
        val hints = diagnostic.hints.map { hint ->
            val source = hint.source
            val offset = if (source == null) sourceContext.inputStart else 0
            CjCanonicalParsingDiagnosticHint(
                resolve(source ?: sourceContext.snapshot, offset + hint.startOffset,
                    offset + hint.endOffset, hint.rangeKind),
                hint.label,
            )
        }
        val normalized = report.copy(diagnostic = diagnostic.copy(
            hints = Collections.unmodifiableList(diagnostic.hints.toList()),
            notes = Collections.unmodifiableList(diagnostic.notes.toList()),
            mainRange = range,
            nativeHints = Collections.unmodifiableList(hints),
        ))
        check(captured.put(report.order, normalized) == null) { "A producer report can only be restored once per collection." }
    }

    /** 全部已保存报告包含被 LEX 门暂时抑制的 PARSE，供编译单元统一发布。 */
    val collectedDiagnostics: List<ParsingDiagnostic>
        get() {
            check(finished) { "A parser diagnostic root must finish before its saved reports are consumed." }
            return completed
        }

    /** 完整规范化输入保留被本根去重的报告，编译单元合并后才能重建同一个 prevDiags。 */
    val collectedReports: List<CjParsingDiagnosticReport>
        get() {
            check(finished) { "A parser diagnostic root must finish before its saved reports are consumed." }
            return completedReports
        }

    val publication: CjParsingDiagnosticPublication
        get() {
            check(finished) { "A parser diagnostic root must finish before publication." }
            return CjParsingDiagnosticPublication(published, completedReports)
        }

    /** 原生 Position 排序不包含 fileID；source namespace 仍保留在两层重复键中。 */
    fun finish(): List<ParsingDiagnostic> {
        check(!finished) { "A parser diagnostic root can only finish once." }
        finished = true
        completedReports = orderReports(captured.values.toList())
        completed = Collections.unmodifiableList(saveReports(completedReports, settings).map { it.diagnostic }.sortedWith(RANGE_ORDER))
        published = publishSyntaxDiagnostics(completedReports, settings)
        return published
    }

    companion object {
        private val RANGE_ORDER = compareBy<ParsingDiagnostic>(
            { checkNotNull(it.mainRange).begin.first }, { checkNotNull(it.mainRange).begin.second },
            { checkNotNull(it.mainRange).end.first }, { checkNotNull(it.mainRange).end.second },
        )

        /** 同根按逻辑调用路径重放；独立根按编译驱动交给发布器的先后合并。 */
        private fun orderReports(reports: List<CjParsingDiagnosticReport>): List<CjParsingDiagnosticReport> {
            val rootOrders = linkedMapOf<UUID, Int>()
            reports.forEach { rootOrders.getOrPut(it.order.rootIdentity) { rootOrders.size } }
            return Collections.unmodifiableList(reports.sortedWith { left, right ->
                val roots = rootOrders.getValue(left.order.rootIdentity).compareTo(rootOrders.getValue(right.order.rootIdentity))
                if (roots != 0) roots else left.order.compareWithinRoot(right.order)
            })
        }

        private fun saveReports(reports: List<CjParsingDiagnosticReport>, settings: ParsingDiagnosticSettings): List<CjParsingDiagnosticReport> {
            val previous = hashSetOf<PreviousKey>()
            val seen = hashSetOf<Key>()
            return Collections.unmodifiableList(orderReports(reports).filter { report ->
                val diagnostic = report.diagnostic
                if (settings.isSuppressed(diagnostic)) return@filter false
                val range = checkNotNull(diagnostic.mainRange) { "Only normalized parser roots can be published." }
                previous.add(PreviousKey(range.source.identity, range.begin, diagnostic.message)) &&
                    seen.add(Key(diagnostic.category, range.source.identity, range.begin, range.end, diagnostic.severity))
            })
        }

        /** 多个真实 parser 根经源码映射后仍先选 producer 首报，再应用编译单元的 LEX 门。 */
        fun publishSyntaxDiagnostics(
            reports: List<CjParsingDiagnosticReport>,
            settings: ParsingDiagnosticSettings = ParsingDiagnosticSettings(),
        ): List<ParsingDiagnostic> {
            val distinct = saveReports(reports, settings).map { it.diagnostic }
            val hasLexicalError = distinct.any { it.category == ParsingDiagnostic.Category.LEX && it.isError }
            val lexical = distinct.filter { it.category == ParsingDiagnostic.Category.LEX }.sortedWith(RANGE_ORDER)
            val parsing = if (hasLexicalError) emptyList() else distinct.filter { it.category == ParsingDiagnostic.Category.PARSE }.sortedWith(RANGE_ORDER)
            return Collections.unmodifiableList(lexical + parsing)
        }
    }

    private fun resolve(
        source: CjParsingSourceSnapshot,
        startOffset: Int,
        endOffset: Int,
        kind: ParsingDiagnostic.RangeKind,
    ): CjParsingDiagnosticRange {
        require(startOffset in 0..endOffset && endOffset <= source.text.length)
        require(kind != ParsingDiagnostic.RangeKind.POINT || startOffset == endOffset)
        val begin = source.nativePosition(startOffset)
        val end = when (kind) {
            ParsingDiagnostic.RangeKind.POINT -> begin.first to begin.second + 1
            // 官方 Token::End 使用 literal 的 UTF-8 字节长度，不将 token 内换行解释为另一个位置。
            ParsingDiagnostic.RangeKind.TOKEN -> begin.first to
                begin.second + source.text.substring(startOffset, endOffset).toByteArray(Charsets.UTF_8).size
            ParsingDiagnostic.RangeKind.RANGE -> source.nativePosition(endOffset)
        }
        return CjParsingDiagnosticRange(source, begin, end,
            org.cangnova.cangjie.parsing.CjParsingSourceRange(source, startOffset, endOffset))
    }
}
