package org.cangnova.cangjie.lsp.completion

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportPlan
import org.cangnova.cangjie.ide.completion.impl.shared.imports.ImportApplyReport
import org.cangnova.cangjie.ide.completion.impl.shared.imports.PsiImportPlanApplier
import org.cangnova.cangjie.lsp.analysis.LspAnalysisPsiFileFactory
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextEdit
import java.awt.EventQueue

/**
 * 计算候选需要的 import 编辑。
 *
 * **不重新实现导入写入规则**：把文本喂给一份**一次性副本**，交给共享的
 * [PsiImportPlanApplier]（它按仓颉语法决定插在哪、能不能并进既有分组、是否已导入），
 * 再把副本的前后文本差折算成一条 LSP 编辑。副本只在这一次调用里存在，
 * LSP 文档本身不被触碰——这正是 15.1② 实验判定过的形态（见 `implementation-log.md`）。
 *
 * 差折算用「最长公共前缀 + 最长公共后缀」：导入应用器**只改导入区**，
 * 因此差异必然连续；这样得到的编辑即使把区间取得偏大也仍然正确（被重写的中间段内容相同），
 * 不会写坏文本，只是粒度粗一点。
 *
 * 已知缺口（显式失败，不静默写坏代码）：`importPlan.conflict != null` 时正确做法是
 * 「改写限定名 + 交给引用缩短」——那需要拿到应用器报告里的 `qualifiedExpressionText`
 * 再决定插入文本，属于插入文本与导入写入的联动，尚未实现。此时本方法抛错，
 * 由调用方转成明确的失败响应，而不是插一个指向别处的短名。
 */
object LspImportEditCalculator {
    /**
     * 计算 [snapshot] 需要的 import 编辑；不需要改 import 时返回空列表。
     *
     * @param project 当前工程（用于取 PSI 与文档管理器）。
     * @param documentUri 触发补全的文档 URI，用于给副本一个可区分的身份。
     * @param fileName 副本文件名（与文档同名，便于包路径判定）。
     * @param text 文档当前文本。
     */
    fun importEdits(
        project: Project,
        documentUri: String,
        fileName: String,
        text: String,
        snapshot: CompletionSnapshot,
    ): List<TextEdit> {
        val plan = snapshot.importPlan.plan as? CompletionImportPlan.AddImport ?: return emptyList()
        // 绑定到局部量：`importPlan.conflict` 是跨模块属性，Kotlin 不做智能转换。
        val conflict = snapshot.importPlan.conflict
        if (conflict != null) {
            error(
                "候选 `${snapshot.lookupString}` 的短名已被占用（${conflict.conflictingBinding}）：" +
                    "「写限定名 + 后续缩短」尚未实现，不能只插短名——那会指向另一个声明。",
            )
        }

        val scratch = LspAnalysisPsiFileFactory.createFile(project, scratchUri(documentUri), fileName, text)
        var report: ImportApplyReport? = null
        // 导入写入要动 PSI 文档：必须在 EDT 的写命令内做（分支 B 的容器约束之一）。
        EventQueue.invokeAndWait {
            WriteCommandAction.runWriteCommandAction(project, "cangjie.completion.import-edit", null, Runnable {
                report = PsiImportPlanApplier(project)
                    .apply(scratch, mapOf(snapshot.identity.deduplicationKey to plan))
            })
        }
        if (report?.documentChanged != true) {
            // 幂等：已导入（或应用器判定无需写入）时不产生编辑，而不是再写一条重复 import。
            return emptyList()
        }

        val document = PsiDocumentManager.getInstance(project).getDocument(scratch)
            ?: error("导入副本没有关联文档，无法读回写入结果")
        return singleHunkEdit(text, document.text)
    }

    /**
     * 副本的 URI。
     *
     * 与文档 URI 不同：两份 PSI 代表同一个源文件，不能共用身份，
     * 否则副本上的写入会与文档快照互相污染。
     */
    private fun scratchUri(documentUri: String): String = "$documentUri#cangjie-import-edit"

    /**
     * 把「原文 → 新文」的差异折算成一条编辑。
     */
    private fun singleHunkEdit(original: String, updated: String): List<TextEdit> {
        if (original == updated) return emptyList()
        val prefix = commonPrefixLength(original, updated)
        val suffix = commonSuffixLength(original, updated, prefix)
        val oldEnd = original.length - suffix
        return listOf(
            TextEdit(
                Range(positionAt(original, prefix), positionAt(original, oldEnd)),
                updated.substring(prefix, updated.length - suffix),
            ),
        )
    }

    /**
     * 最长公共前缀长度。
     */
    private fun commonPrefixLength(original: String, updated: String): Int {
        var index = 0
        val limit = minOf(original.length, updated.length)
        while (index < limit && original[index] == updated[index]) index++
        return index
    }

    /**
     * 最长公共后缀长度；不与前缀区间重叠。
     */
    private fun commonSuffixLength(original: String, updated: String, prefix: Int): Int {
        var length = 0
        val limit = minOf(original.length, updated.length) - prefix
        while (length < limit && original[original.length - 1 - length] == updated[updated.length - 1 - length]) {
            length++
        }
        return length
    }

    /**
     * 偏移 → LSP 位置（`\r\n` 下 `\r` 不计入列号）。
     */
    private fun positionAt(text: String, offset: Int): Position {
        var line = 0
        var lineStart = 0
        var index = 0
        while (index < offset && index < text.length) {
            if (text[index] == '\n') {
                line++
                lineStart = index + 1
            }
            index++
        }
        var character = offset - lineStart
        if (character > 0 && offset - 1 < text.length && text[offset - 1] == '\r') character--
        return Position(line, character)
    }
}
