package org.cangnova.cangjie.psi.psiUtil

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjMacroInput

/**
 * 返回声明宏输入所在的真实宿主容器。
 *
 * AST 的 declaration → MACRO_INPUT → MACRO_EXPRESSION 和省略 MACRO_INPUT 的 stub
 * 使用同一条所属关系。声明包装不建立作用域；括号 Tokens 输入仍是表达式边界。
 * 此关系只确定输入的宿主，不能把未记录的 construction carrier 当成展开后的声明。
 */
fun unwrapDeclarationMacroInputParent(parent: PsiElement?): PsiElement? {
    var container = parent
    while (true) {
        when (val current = container) {
            is CjMacroInput -> {
                val macro = current.parent as? CjMacroExpression ?: return current
                if (macro.input !== current || !macro.hasDeclarationInput) return current
                container = macro.parent
            }
            is CjMacroExpression -> {
                if (!current.hasDeclarationInput) return current
                container = current.parent
            }
            else -> return current
        }
    }
}
