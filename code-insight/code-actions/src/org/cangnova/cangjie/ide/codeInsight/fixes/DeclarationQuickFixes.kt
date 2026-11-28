package org.cangnova.cangjie.ide.codeInsight.fixes

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.Presentation
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjImportDirective
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.CjMatchEntry
import org.cangnova.cangjie.psi.CjMatchExpression
import org.cangnova.cangjie.psi.CjPsiFactory

/**
 * 「移除 import」修复。
 *
 * 诊断 PSI 是 [CjImportItem]：分组 import 中只删叶项（[CjImportItem.delete]
 * 会维护逗号分隔符，组内其余成员保留）；独立语句中叶项删除会级联删掉整句。
 */
internal class RemoveImportQuickFix(
    /** 要移除的导入叶项。 */
    private val importItem: CjImportItem,
) : CangJiePsiUpdateQuickFix<CjImportItem>(importItem, FAMILY_NAME) {
    override fun getPresentation(context: ActionContext, element: CjImportItem): Presentation =
        Presentation.of("Remove unused import")

    override fun invoke(context: ActionContext, element: CjImportItem, updater: ModPsiUpdater) {
        element.delete()
    }

    companion object {
        /** family 名称；导入类修复共用。 */
        const val FAMILY_NAME: String = "CangJie import quick fixes"
    }
}

/**
 * 「为 match 表达式补缺失分支」修复。
 *
 * 分支文本由穷尽性分析器的规范化模式给出（如 `A`、`B`、`_`），
 * 统一生成 `case X => ()` 形态的分支并插入到最后一个分支之后，
 * 由用户再填充分支体。缺失文本含 ` 或以大写开头时按 enum/常量模式原样使用；
 * 通配 `_` 场景生成 `case _ => ()`（else 分支的显式写法）。
 */
internal class AddMatchBranchQuickFix(
    /** 要补分支的 match 表达式。 */
    private val matchExpression: CjMatchExpression,
    /** 缺失的模式文本。 */
    private val missingCaseText: String,
) : CangJiePsiUpdateQuickFix<CjMatchExpression>(matchExpression, FAMILY_NAME) {
    override fun getPresentation(context: ActionContext, element: CjMatchExpression): Presentation =
        Presentation.of("Add match branch for '$missingCaseText'")

    override fun invoke(context: ActionContext, element: CjMatchExpression, updater: ModPsiUpdater) {
        val entry = CjPsiFactory(context.project).createMatchEntry("$missingCaseText => ()")
        val anchor = element.entries.lastOrNull() ?: return
        val newEntry = element.addAfter(entry, anchor) as CjMatchEntry
        // 与上一分支之间补一个换行，保持分支逐行排布。
        val previousText = anchor.text
        if (!previousText.endsWith("\n") && anchor.nextSibling !is com.intellij.psi.PsiWhiteSpace) {
            element.addAfter(CjPsiFactory(context.project).createNewLine(), anchor)
        }
        // 光标位置作为 ModCommand 的导航副作用记录，由 IDE/LSP host 各自消费。
        (newEntry.body ?: newEntry.expression)?.let { body ->
            updater.moveCaretTo(body.textRange.startOffset + 1)
        }
    }

    companion object {
        /** family 名称；match 类修复共用。 */
        const val FAMILY_NAME: String = "CangJie match quick fixes"
    }
}

/**
 * 「按 replacement 替换弃用使用点」修复。
 *
 * 仓颉 `@Deprecated[replacement = "..."]` 的 replacement 是一段源码片段
 * （官方语义为推荐替代写法），修复把诊断定位的使用点 PSI 整体替换为该片段。
 */
internal class ReplaceWithQuickFix(
    /** 诊断定位的使用点 PSI。 */
    private val usageElement: PsiElement,
    /** 被弃用声明的名称，仅用于展示。 */
    private val deprecatedName: String,
    /** 注解给出的替换片段。 */
    private val replacement: String,
) : CangJiePsiUpdateQuickFix<PsiElement>(usageElement, FAMILY_NAME) {
    override fun getPresentation(context: ActionContext, element: PsiElement): Presentation =
        Presentation.of("Replace deprecated '$deprecatedName' with '$replacement'")

    override fun invoke(context: ActionContext, element: PsiElement, updater: ModPsiUpdater) {
        element.replace(CjPsiFactory(context.project).createExpression(replacement))
    }

    companion object {
        /** family 名称；弃用替换类修复共用。 */
        const val FAMILY_NAME: String = "CangJie deprecation quick fixes"
    }
}
