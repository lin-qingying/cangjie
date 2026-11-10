package org.cangnova.cangjie.ide.codeInsight.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
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
    val useExprWithoutImport: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.UseExprWithoutImport> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            val file = diagnostic.psi.containingFile as? CjFile ?: return@IntentionBased emptyList()
            val path = ImportPath(diagnostic.importPath, isAllUnder = false)
            listOf(AddImportQuickFix(file, path))
        }

    /**
     * `UnresolvedImport`：import 目标解析失败，提供「移除该 import 项」修复。
     *
     * 分组 import（`import a.{b, c}`）中只删未解析的叶项（[CjImportItem.delete] 会维护逗号分隔符），
     * 其余可解析成员不受影响；独立 import 语句则整句删除。
     */
    val unresolvedImport: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.UnresolvedImport> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            val container = diagnostic.psi.containingImportEntry()
            if (!container.isValid) return@IntentionBased emptyList()
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
) : IntentionAction {
    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = FAMILY_NAME

    override fun getText(): String = "Add import '$importPath'"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        file is CjFile && file.isValid

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val cjFile = file as? CjFile ?: return
        // 与补全链路一致：导入写入必须在写命令内，保证一次撤销覆盖整条修复。
        WriteCommandAction.runWriteCommandAction(project, FAMILY_NAME, null, Runnable {
            val plan = CompletionImportPlan.AddImport(
                // 表达式文本与本修复无关（不改使用点），填短名仅为满足契约。
                CompletionImportTargetData(importPath, importPath.importedName?.asString().orEmpty()),
            )
            PsiImportPlanApplier(project).apply(cjFile, mapOf(importPath.toString() to plan))
        })
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
) : IntentionAction {
    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = FAMILY_NAME

    override fun getText(): String = "Remove unresolved import"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        file is CjFile && importEntry.isValid

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (!importEntry.isValid) return
        WriteCommandAction.runWriteCommandAction(project, FAMILY_NAME, null, Runnable {
            importEntry.delete()
        })
    }

    companion object {
        /** family 名称；与 [AddImportQuickFix] 同族。 */
        const val FAMILY_NAME: String = AddImportQuickFix.FAMILY_NAME
    }
}
