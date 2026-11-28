package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import org.cangnova.cangjie.messages.CjParsingDiagnosticOrder
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjRelativeErrorHint
import org.cangnova.cangjie.psi.CjTokenRangeErrorElement

/**
 * 不消费当前词元的语法诊断。报告范围独立于零宽文法节点，保留原生恢复停点和定界符身份。
 * 每次报告的消息和宽度属于本次树；动态类型不进入 IntelliJ 全局 element-type 注册表。
 */
class CjTokenRangeErrorElementType(
    override val message: String,
    val reportedWidth: Int,
    hintRanges: List<CjRelativeErrorHint> = emptyList(),
    val category: ParsingDiagnostic.Category = ParsingDiagnostic.Category.PARSE,
    diagnosticOrder: CjParsingDiagnosticOrder,
) :
    CjParsingErrorElementType("TOKEN_RANGE_ERROR", diagnosticOrder, hintRanges) {
    /** Token 范围保存当前词元锚点，不将前置 trivia 移入这个零宽载体。 */
    override fun isLeftBound(): Boolean = false

    init {
        require(reportedWidth > 0)
    }

    override fun createCompositeNode(): ASTNode = CjTokenRangeErrorElement(message, reportedWidth, hintRanges, category, diagnosticOrder)

    override fun diagnostic(nodeStart: Int, nodeEnd: Int): ParsingDiagnostic =
        ParsingDiagnostic(nodeStart, nodeStart + reportedWidth, message, hints = reportedHints(nodeStart),
            category = category, rangeKind = ParsingDiagnostic.RangeKind.TOKEN)

}
