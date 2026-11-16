package org.cangnova.cangjie.ide.codeInsight.fixes

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.Presentation
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CaCfirDiagnostic
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixFactory
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportPlan
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportTargetData
import org.cangnova.cangjie.ide.completion.impl.shared.imports.PsiImportPlanApplier
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportDirective
import org.cangnova.cangjie.psi.CjImportItem

/**
 * import 族诊断的 K2 quick-fix factory 集合。
 *
 * 复用补全链路已验证的 [PsiImportPlanApplier] 承担导入写入（幂等、分组并入、冲突报告），
 * factory 只负责「诊断 → 导入计划」的映射，不重复实现导入策略。
 */
object ImportQuickFixFactories {
    /**
     * `UseExprWithoutImport`：诊断已携带 [CaCfirDiagnostic.UseExprWithoutImport.importPath]，
     * 直接为该路径补一条声明级 import。
     */
    val useExprWithoutImport: CangJieQuickFixFactory.ModCommandBased<CaCfirDiagnostic.UseExprWithoutImport> =
        CangJieQuickFixFactory.ModCommandBased { diagnostic ->
            val file = diagnostic.psi.containingFile as? CjFile ?: return@ModCommandBased emptyList()
            val path = ImportPath(diagnostic.importPath, isAllUnder = false)
            listOf(AddImportQuickFix(file, path))
        }

    /**
     * `UnresolvedImport`：import 目标解析失败，提供「移除该 import 项」修复。
     *
     * 分组 import（`import a.{b, c}`）中只删未解析的叶项（[CjImportItem.delete] 会维护逗号分隔符），
     * 其余可解析成员不受影响；独立 import 语句则整句删除。
     */
    val unresolvedImport: CangJieQuickFixFactory.ModCommandBased<CaCfirDiagnostic.UnresolvedImport> =
        CangJieQuickFixFactory.ModCommandBased { diagnostic ->
            val container = diagnostic.psi.containingImportEntry()
            if (!container.isValid) return@ModCommandBased emptyList()
            listOf(RemoveUnresolvedImportQuickFix(container))
        }

    /**
     * 从诊断 PSI 向上找最近的导入叶项或整句导入容器。
     *
     * 诊断定位到 import 的失败段（如 `import ghost.pkg.Missing` 中的 `ghost`），
     * 修复操作的对象必须是可整体删除的语法单元。
     */
    private fun PsiElement.containingImportEntry(): PsiElement =
        generateSequence(this) { it.parent }
            .firstOrNull { it is CjImportItem || it is CjImportDirective }
            ?: this
}

/**
 * 「补一条 import」修复。
 *
 * 写入经由 [PsiImportPlanApplier]：已存在等价 import 时幂等跳过，短名冲突时不硬插。
 */
internal class AddImportQuickFix(
    /** 诊断所在文件；导入写入锚点（import 区末尾 / 包声明之后）由 applier 决定。 */
    private val targetFile: CjFile,
    /** 要追加的导入路径。 */
    private val importPath: ImportPath,
) : CangJiePsiUpdateQuickFix<CjFile>(targetFile, FAMILY_NAME) {
    override fun getPresentation(context: ActionContext, element: CjFile): Presentation =
        Presentation.of("Add import '$importPath'")

    override fun invoke(context: ActionContext, element: CjFile, updater: ModPsiUpdater) {
        val plan = CompletionImportPlan.AddImport(
            // 表达式文本与本修复无关（不改使用点），填短名仅为满足契约。
            CompletionImportTargetData(importPath, importPath.importedName?.asString().orEmpty()),
        )
        PsiImportPlanApplier(context.project).apply(element, mapOf(importPath.toString() to plan))
    }

    companion object {
        /** family 名称；同一批导入类修复共用，便于平台按族分组。 */
        const val FAMILY_NAME: String = "CangJie import quick fixes"
    }
}

/**
 * 「移除未解析 import」修复。
 */
internal class RemoveUnresolvedImportQuickFix(
    /** 要移除的导入叶项或整句导入容器。 */
    private val importEntry: PsiElement,
) : CangJiePsiUpdateQuickFix<PsiElement>(importEntry, FAMILY_NAME) {
    override fun getPresentation(context: ActionContext, element: PsiElement): Presentation =
        Presentation.of("Remove unresolved import")

    override fun invoke(context: ActionContext, element: PsiElement, updater: ModPsiUpdater) {
        element.delete()
    }

    companion object {
        /** family 名称；与 [AddImportQuickFix] 同族。 */
        const val FAMILY_NAME: String = AddImportQuickFix.FAMILY_NAME
    }
}
