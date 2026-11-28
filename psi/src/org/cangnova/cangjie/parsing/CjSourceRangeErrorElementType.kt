package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import org.cangnova.cangjie.messages.CjParsingDiagnosticOrder
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjRelativeErrorHint
import org.cangnova.cangjie.psi.CjSourceRangeErrorElement

/** 共享 parser 的零宽错误节点类型；PSI 与 LightTree 读取同一条源码范围事实。 */
class CjSourceRangeErrorElementType(
    override val message: String,
    val reportedStartDelta: Int,
    val reportedWidth: Int,
    hintRanges: List<CjRelativeErrorHint> = emptyList(),
    val reportedSource: CjParsingSourceSnapshot? = null,
    val category: ParsingDiagnostic.Category = ParsingDiagnostic.Category.PARSE,
    val rangeKind: ParsingDiagnostic.RangeKind = ParsingDiagnostic.RangeKind.RANGE,
    diagnosticOrder: CjParsingDiagnosticOrder,
) : CjParsingErrorElementType("SOURCE_RANGE_ERROR", diagnosticOrder, hintRanges) {
    /** 显式范围以逻辑 lookahead 为相对锚点；不采用 SDK 原生空错误向前吸收 trivia 的规则。 */
    override fun isLeftBound(): Boolean = false

    init {
        require(reportedWidth >= 0)
    }

    override fun createCompositeNode(): ASTNode =
        CjSourceRangeErrorElement(message, reportedStartDelta, reportedWidth, hintRanges, reportedSource, category, rangeKind,
            diagnosticOrder)

    override fun diagnostic(nodeStart: Int, nodeEnd: Int): ParsingDiagnostic {
        val start = (if (reportedSource == null) nodeStart else 0) + reportedStartDelta
        return ParsingDiagnostic(start, start + reportedWidth, message, hints = reportedHints(nodeStart),
            source = reportedSource, category = category, rangeKind = rangeKind)
    }

}
