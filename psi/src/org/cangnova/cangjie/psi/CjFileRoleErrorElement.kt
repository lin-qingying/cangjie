package org.cangnova.cangjie.psi

import com.intellij.psi.impl.source.tree.PsiErrorElementImpl
import org.cangnova.cangjie.messages.CjParsingDiagnosticOrder
import org.cangnova.cangjie.messages.ParsingDiagnosticHint

/** 文件角色错误仍是 IDE 的语法错误节点，同时明确它属于原文件而非宏生成片段。 */
class CjFileRoleErrorElement(
    message: String,
    override val diagnosticOrder: CjParsingDiagnosticOrder,
) : PsiErrorElementImpl(message), CjOrderedParsingErrorElement {
    override val reportedHints: List<ParsingDiagnosticHint> = emptyList()
}
