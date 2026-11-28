package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.tree.ICompositeElementType
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.messages.CjParsingDiagnosticOrder
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.psi.CjRelativeErrorHint
import org.cangnova.cangjie.psi.CjSyntaxErrorElement
import org.cangnova.cangjie.psi.freezeRelativeErrorHints

/**
 * 所有 parser 错误的同次报告载体。LightTree 保存类型化身份，实际 PSI 保持 ERROR_ELEMENT；
 * 动态类型不全局注册，也不会进入 SDK 按消息文本提前复用原生错误的比较分支。
 */
abstract class CjParsingErrorElementType(
    debugName: String,
    val diagnosticOrder: CjParsingDiagnosticOrder,
    hintRanges: List<CjRelativeErrorHint> = emptyList(),
) : IElementType(debugName, CangJieLanguage, false), ICompositeElementType {
    /** 普通、token 与独立来源错误共用不可变提示载体，类别和主范围仍由各文法入口决定。 */
    val hintRanges: List<CjRelativeErrorHint> = freezeRelativeErrorHints(hintRanges)

    abstract val message: String
    /** SDK processDone 对纯 trivia/空错误使用原生 ERROR_ELEMENT 的左绑定规则。 */
    override fun isLeftBound(): Boolean = true
    val astElementType: IElementType get() = TokenType.ERROR_ELEMENT
    abstract fun diagnostic(nodeStart: Int, nodeEnd: Int): ParsingDiagnostic

    fun reportedHints(nodeStartOffset: Int): List<ParsingDiagnosticHint> =
        hintRanges.map { it.toParsingDiagnosticHint(nodeStartOffset) }
}

/** SDK error/errorBefore 的结构范围由最终 marker 范围确定，报告身份由调用点确定。 */
class CjSyntaxErrorElementType(
    override val message: String,
    diagnosticOrder: CjParsingDiagnosticOrder,
    hintRanges: List<CjRelativeErrorHint> = emptyList(),
) : CjParsingErrorElementType("SYNTAX_ERROR", diagnosticOrder, hintRanges) {
    override fun createCompositeNode(): ASTNode = CjSyntaxErrorElement(message, diagnosticOrder, this.hintRanges)
    override fun diagnostic(nodeStart: Int, nodeEnd: Int): ParsingDiagnostic =
        ParsingDiagnostic(nodeStart, nodeEnd, message, hints = reportedHints(nodeStart))
}

/** PSI/LT 实际转换的类型对位；错误载体只规范为 SDK ERROR_ELEMENT，不按消息选择节点。 */
internal fun IElementType.actualAstElementType(): IElementType =
    if (this is CjParsingErrorElementType) astElementType else this

/** raw 结构消费统一识别 SDK 错误与带 producer 身份的错误，不以特定动态类型枚举遗漏包装。 */
fun IElementType.isParsingErrorElementType(): Boolean = this == TokenType.ERROR_ELEMENT || this is CjParsingErrorElementType
