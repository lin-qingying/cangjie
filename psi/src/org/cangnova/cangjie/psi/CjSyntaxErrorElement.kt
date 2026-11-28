package org.cangnova.cangjie.psi

import com.intellij.psi.impl.source.tree.PsiErrorElementImpl
import org.cangnova.cangjie.messages.CjParsingDiagnosticOrder
import org.cangnova.cangjie.messages.ParsingDiagnosticHint

/** 实际错误节点携带 producer 身份；复制保留同次报告，重新解析产生新的类型化节点。 */
interface CjOrderedParsingErrorElement {
    val diagnosticOrder: CjParsingDiagnosticOrder
    /** 每个提示保留自身来源与范围；PSI 与轻树读取同次冻结报告。 */
    val reportedHints: List<ParsingDiagnosticHint>
}

/** 保留 SDK 的 ERROR_ELEMENT、真实子节点与 textRange，专有属性只记录诊断调用顺序。 */
class CjSyntaxErrorElement(
    message: String,
    override val diagnosticOrder: CjParsingDiagnosticOrder,
    hintRanges: List<CjRelativeErrorHint> = emptyList(),
) : PsiErrorElementImpl(message), CjOrderedParsingErrorElement {
    private val hintRanges = freezeRelativeErrorHints(hintRanges)

    override val reportedHints: List<ParsingDiagnosticHint>
        get() = hintRanges.map { it.toParsingDiagnosticHint(startOffset) }
}
