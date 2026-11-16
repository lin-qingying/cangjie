package org.cangnova.cangjie.ide.codeInsight.fixes

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.Presentation
import com.intellij.psi.PsiElement
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
) : CangJiePsiUpdateQuickFix<PsiElement>(contextElement, FAMILY_NAME) {
    override fun getPresentation(context: ActionContext, element: PsiElement): Presentation? {
        if (containingDeclaration(element) !is CjModifierListOwner) return null
        return Presentation.of(displayText)
    }

    override fun invoke(context: ActionContext, element: PsiElement, updater: ModPsiUpdater) {
        val declaration = containingDeclaration(element) as? CjModifierListOwner ?: return
        removeModifier(declaration, token)
    }

    /**
     * 从诊断 PSI 向上找最近的声明：修饰符诊断可能定位在修饰符节点、
     * 声明关键字或声明本体上，三者都收敛到同一个 [CjModifierListOwner]。
     */
    private fun containingDeclaration(element: PsiElement): PsiElement? =
        generateSequence(element) { it.parent }.firstOrNull { it is CjModifierListOwner }

    companion object {
        /** family 名称；修饰符类修复共用。 */
        const val FAMILY_NAME: String = "CangJie modifier quick fixes"
    }
}
