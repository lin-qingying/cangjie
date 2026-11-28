package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import org.cangnova.cangjie.CjmpFileRoleError
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.CjParsingDiagnosticOrder
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjFileRoleErrorElement

/** 官方 CheckCJMPModifiers 报在 currentFile；树节点保留这个归属，不借消息文字识别。 */
class CjmpFileRoleErrorElementType(
    val roleError: CjmpFileRoleError,
    diagnosticOrder: CjParsingDiagnosticOrder,
) : CjParsingErrorElementType(roleError.name, diagnosticOrder) {
    override fun isLeftBound(): Boolean = false

    override val message: String get() = CangJieParsingBundle.message(when (roleError) {
        CjmpFileRoleError.UNEXPECTED_CJMP_DECL -> "parsing.error.cjmp.unexpected.declaration"
        CjmpFileRoleError.COMMON_IN_NON_COMMON -> "parsing.error.cjmp.common.in.non.common.file"
        CjmpFileRoleError.SPECIFIC_IN_NON_SPECIFIC -> "parsing.error.cjmp.specific.in.non.specific.file"
        CjmpFileRoleError.COMMON_AND_SPECIFIC -> "parsing.error.cjmp.common.and.specific.in.same.file"
    })

    /** 实体 AST 使用 IntelliJ 原生 ERROR_ELEMENT，保留现有 IDE 错误检测协议。 */
    override fun createCompositeNode(): ASTNode = CjFileRoleErrorElement(message, diagnosticOrder)

    override fun diagnostic(nodeStart: Int, nodeEnd: Int): ParsingDiagnostic =
        ParsingDiagnostic(nodeStart, nodeEnd, message, ParsingDiagnostic.Origin.ORIGINAL_FILE)
}
