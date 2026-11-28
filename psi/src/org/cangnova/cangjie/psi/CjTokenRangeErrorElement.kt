package org.cangnova.cangjie.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.source.tree.PsiErrorElementImpl
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.CjParsingDiagnosticOrder

/**
 * 实体错误仍遵守 SDK 的零宽 AST 协议；报告范围覆盖被诊断词元，不包裹或消费该词元。
 * 不覆盖 textRange，避免改变 PSI 子节点长度、直接定界符身份及增量合并的不变量。
 */
class CjTokenRangeErrorElement(
    message: String,
    val reportedWidth: Int,
    hintRanges: List<CjRelativeErrorHint> = emptyList(),
    val category: ParsingDiagnostic.Category = ParsingDiagnostic.Category.PARSE,
    override val diagnosticOrder: CjParsingDiagnosticOrder,
) : PsiErrorElementImpl(message), CjOrderedParsingErrorElement {
    private val hintRanges = freezeRelativeErrorHints(hintRanges)

    init {
        require(reportedWidth > 0)
    }

    val reportedRange: TextRange get() = TextRange.from(startOffset, reportedWidth)

    override val reportedHints: List<ParsingDiagnosticHint>
        get() = hintRanges.map { it.toParsingDiagnosticHint(startOffset) }
}
