package org.cangnova.cangjie.ide.codeInsight.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.psi.psiUtil.removeModifier

/**
 * 「删除一个修饰符」修复。
 *
 * 诊断携带的 [token] 是 [CjKeywordToken]（如 `OPEN_KEYWORD`），修复直接复用
 * psiUtil 的 [removeModifier]：从最近的 [CjModifierListOwner] 的 modifierList
 * 中删除该关键字节点；modifierList 因此清空时由 psiUtil 一并删除。
 */
internal class RemoveModifierQuickFix(
    /** 诊断定位的 PSI 元素；修复对象从它向上找最近的声明。 */
    private val contextElement: PsiElement,
    /** 要删除的修饰符关键字 token。 */
    private val token: CjKeywordToken,
    /** 修复展示文本（描述删除哪个修饰符）。 */
    private val displayText: String,
) : IntentionAction {
    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = FAMILY_NAME

    override fun getText(): String = displayText

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        file != null && contextElement.isValid && containingDeclaration() is CjModifierListOwner

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val declaration = containingDeclaration() as? CjModifierListOwner ?: return
        WriteCommandAction.runWriteCommandAction(project, FAMILY_NAME, null, Runnable {
            removeModifier(declaration, token)
        })
    }

    /**
     * 从诊断 PSI 向上找最近的声明：修饰符诊断可能定位在修饰符节点、
     * 声明关键字或声明本体上，三者都收敛到同一个 [CjModifierListOwner]。
     */
    private fun containingDeclaration(): PsiElement? =
        generateSequence(contextElement) { it.parent }.firstOrNull { it is CjModifierListOwner }

    companion object {
        /** family 名称；修饰符类修复共用。 */
        const val FAMILY_NAME: String = "CangJie modifier quick fixes"
    }
}
