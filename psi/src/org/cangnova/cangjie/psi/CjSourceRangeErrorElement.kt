package org.cangnova.cangjie.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.source.tree.PsiErrorElementImpl
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.CjParsingDiagnosticOrder
import org.cangnova.cangjie.parsing.CjParsingSourceSnapshot

/** AST 节点保持零宽；报告的源码范围由共享 parser 明确记录。 */
class CjSourceRangeErrorElement(
    message: String,
    val reportedStartDelta: Int,
    val reportedWidth: Int,
    hintRanges: List<CjRelativeErrorHint> = emptyList(),
    val reportedSource: CjParsingSourceSnapshot? = null,
    val category: ParsingDiagnostic.Category = ParsingDiagnostic.Category.PARSE,
    val rangeKind: ParsingDiagnostic.RangeKind = ParsingDiagnostic.RangeKind.RANGE,
    override val diagnosticOrder: CjParsingDiagnosticOrder,
) : PsiErrorElementImpl(message), CjOrderedParsingErrorElement {
    private val hintRanges = freezeRelativeErrorHints(hintRanges)

    init {
        require(reportedWidth >= 0)
    }

    val reportedRange: TextRange
        get() {
            val reportedStart = (if (reportedSource == null) startOffset else 0) + reportedStartDelta
            require(reportedStart >= 0)
            return TextRange.from(reportedStart, reportedWidth)
        }

    override val reportedHints: List<ParsingDiagnosticHint>
        get() = hintRanges.map { it.toParsingDiagnosticHint(startOffset) }
}

/** 错误节点上的辅助位置相对于节点起点存储，便于增量树移动时保持有效。 */
data class CjRelativeErrorHint(
    val startOffsetDelta: Int,
    val endOffsetDelta: Int,
    val label: String? = null,
    val source: CjParsingSourceSnapshot? = null,
    val rangeKind: ParsingDiagnostic.RangeKind = ParsingDiagnostic.RangeKind.RANGE,
) {
    init {
        require(startOffsetDelta <= endOffsetDelta)
        require(rangeKind != ParsingDiagnostic.RangeKind.POINT || startOffsetDelta == endOffsetDelta)
    }

    /** 本地提示跟随实际载体移动；独立来源提示继续使用该来源的绝对坐标。 */
    fun toParsingDiagnosticHint(anchor: Int): ParsingDiagnosticHint {
        val basis = if (source == null) anchor else 0
        return ParsingDiagnosticHint(basis + startOffsetDelta, basis + endOffsetDelta, label, source, rangeKind)
    }
}

/** 入载体即冻结，调用方随后修改列表不得改变已报告的提示。 */
internal fun freezeRelativeErrorHints(hints: List<CjRelativeErrorHint>): List<CjRelativeErrorHint> =
    java.util.Collections.unmodifiableList(hints.toList())
