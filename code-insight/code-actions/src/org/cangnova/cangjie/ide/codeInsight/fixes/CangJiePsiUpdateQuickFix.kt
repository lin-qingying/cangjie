package org.cangnova.cangjie.ide.codeInsight.fixes

import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.psi.PsiElement

/**
 * 仓颉 quick-fix 的 PSI 更新基类。
 *
 * 修复通过平台 ModCommand 在副本 PSI 上修改文件，使 IDE 与 LSP 共用同一份更新逻辑。
 */
internal abstract class CangJiePsiUpdateQuickFix<E : PsiElement>(
    element: E,
    private val quickFixFamilyName: String,
) : PsiUpdateModCommandAction<E>(element) {
    override fun getFamilyName(): String = quickFixFamilyName
}
