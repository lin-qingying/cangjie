package org.cangnova.cangjie.lsp.analysis

import com.intellij.codeInsight.intention.IntentionAction
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixService
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.jsonrpc.messages.Either

/**
 * 把 quick-fix factory 产出的修复转换成 LSP code action（P3 接线）。
 *
 * 与 IDE 共用同一批 factory：诊断集合直接送 `CangJieQuickFixService` 的
 * catching 版本通道，产出 [IntentionAction] 后按「title + 关联诊断」投影为
 * [CodeAction]。修复的实际写入是编辑器语义（PSI 写事务），LSP 客户端无法
 * 直接执行，因此 action 以命令形式下发（title + 诊断码），由 LSP 宿主的
 * apply-command 处理器回到 IDE 语义执行。
 */
internal object AnalysisApiCodeActions {

    /**
     * 为请求范围覆盖的诊断计算 quick fix。
     *
     * [session] 是当前文档快照的分析会话；[diagnostics] 必须来自同一 session。
     * LSP 请求范围（字符区间）与 PSI 偏移对齐后按交集筛选，避免为整个文件
     * 的诊断全部生成 action。
     */
    fun toCodeActions(
        session: CaSession,
        document: LspTextDocument,
        requestStartOffset: Int,
        requestEndOffset: Int,
        diagnostics: List<CaDiagnosticWithPsi<*>>,
    ): List<Either<Command, CodeAction>> {
        val quickFixService = CangJieQuickFixService.getInstance()
        return diagnostics
            .filter { diagnostic ->
                val range = diagnostic.psi.textRange ?: return@filter false
                range.startOffset <= requestEndOffset && range.endOffset >= requestStartOffset
            }
            .flatMap { diagnostic ->
                with(quickFixService) {
                    // 与 IDE 高亮链同一 catching 版本：单个 factory 失败只丢该条 action。
                    session.getQuickFixesWithCatchingFor(diagnostic)
                }.mapNotNull { result ->
                    result.fold(
                        onSuccess = { fix -> diagnostic.toCodeAction(document, fix) },
                        onFailure = { null },
                    )
                }
            }
    }

    /**
     * 把单条修复投影为 quick-fix 类型的 code action。
     */
    private fun CaDiagnosticWithPsi<*>.toCodeAction(
        document: LspTextDocument,
        fix: IntentionAction,
    ): Either<Command, CodeAction> {
        val action = CodeAction(fix.text).apply {
            kind = CodeActionKind.QuickFix
            // 关联诊断让客户端把 action 与对应 diagnostic 高亮配对；
            // 范围取诊断首范围，与 LSP diagnostics 上报口径一致。
            diagnostics = listOf(
                Diagnostic().apply {
                    range = document.analysisRangeOf(firstRange().startOffset, firstRange().endOffset)
                    code = org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft(factoryName)
                },
            )
        }
        return Either.forRight(action)
    }

    /**
     * 取诊断首个有效文本范围；诊断只挂 PSI 时回退元素范围。
     */
    private fun CaDiagnosticWithPsi<*>.firstRange(): com.intellij.openapi.util.TextRange {
        textRanges.firstOrNull { !it.isEmpty }?.let { return it }
        return psi.textRange ?: com.intellij.openapi.util.TextRange.EMPTY_RANGE
    }
}
