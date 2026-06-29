package org.cangnova.cangjie.lsp.analysis

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.openapi.application.ApplicationManager
import org.cangnova.cangjie.codeinsight.refactoring.rename.CangJieHeadlessRenamer
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior
import org.eclipse.lsp4j.PrepareRenameResult
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.RenameParams
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either3
import java.awt.EventQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * Analysis API 驱动的 LSP rename 协议适配层。
 *
 * 真正的 rename 执行在 `code-insight:refactoring` 的 [CangJieHeadlessRenamer]。
 * 本类只负责 LSP 位置解析、prepareRename 范围返回，以及把重构后的 PSI 文本差异转换为
 * LSP `WorkspaceEdit`。LSP 不再维护独立引用扫描或手写 target-key rename。
 */
internal class AnalysisApiLspRenameSupport(
    /**
     * 提供 PSI 快照分析、目标元素查找和文档 URI 反查的语义支持对象。
     */
    private val semanticSupport: AnalysisApiLspSemanticSupport,
) {
    /**
     * 执行 LSP rename 请求并返回工作区编辑。
     *
     * 方法先解析当前位置可重命名目标，再调用 headless renamer 修改 PSI，最后把所有受影响文件的文本差异转换为 LSP edit。
     */
    fun rename(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: RenameParams,
    ): WorkspaceEdit? {
        if (params.newName.isBlank()) return null

        val target = semanticSupport.analyzeSnapshot(document) { snapshot ->
            collectRenameTargetElement(document, snapshot.psiFile, params.position)
        } ?: return null

        val renamer = CangJieHeadlessRenamer(
            project = target.project,
            target = target,
            newName = params.newName,
            searchInComments = false,
            searchTextOccurrences = false,
        )
        runInEdtWriteAction {
            renamer.rename()
        }

        val changes = linkedMapOf<String, MutableList<TextEdit>>()
        renamer.originals.values.forEach { (file, originalText) ->
            val uri = (file as? CjFile)?.let(semanticSupport::documentUriOf) ?: file.virtualFile?.url ?: return@forEach
            val newText = file.text
            if (newText == originalText) return@forEach

            val originalDocument = LspTextDocument(
                uri = uri,
                languageId = document.languageId,
                version = semanticSupport.openedDocument(file as? CjFile ?: return@forEach)?.version ?: 0,
                text = originalText,
            )
            changes.getOrPut(uri, ::mutableListOf) += LspTextEditsComputer.computeTextEdits(
                document = originalDocument,
                oldText = originalText,
                newText = newText,
            )
        }

        if (changes.isEmpty()) return null

        return WorkspaceEdit().apply {
            this.changes = changes
        }
    }

    /**
     * 执行 prepareRename 请求并返回可重命名名称范围。
     *
     * 返回值包含客户端可展示的占位文本和精确范围；当前位置不可重命名时返回 null。
     */
    fun prepareRename(
        document: LspTextDocument,
        params: RenameParams,
    ): Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>? {
        return semanticSupport.analyzeSnapshot(document) { snapshot ->
            val renameTarget = collectRenameTarget(document, snapshot.psiFile, params.position) ?: return@analyzeSnapshot null
            Either3.forSecond(
                PrepareRenameResult(
                    renameTarget.range,
                    renameTarget.placeholder,
                ),
            )
        }
    }

    /**
     * 收集 prepareRename 需要的目标名称和范围。
     *
     * 方法同时确认当前位置存在可重命名 PSI 元素，避免只在普通标识符文本上返回误导性范围。
     */
    private fun collectRenameTarget(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): RenameTarget? {
        val nameElement = findRenameNameElement(document, file, position) ?: return null
        collectRenameTargetElement(document, file, position) ?: return null

        return RenameTarget(
            placeholder = nameElement.text,
            range = document.analysisRangeOf(
                nameElement.textRange.startOffset,
                nameElement.textRange.endOffset,
            ),
        )
    }

    /**
     * 在指定位置查找可由 headless renamer 处理的 PSI 元素。
     */
    private fun collectRenameTargetElement(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): PsiElement? {
        return semanticSupport.findTargetElements(document, file, position)
            .firstOrNull(CangJieHeadlessRenamer::canRename)
    }

    /**
     * 查找用于 prepareRename 展示范围的名称 PSI 元素。
     *
     * 优先选择声明的 nameIdentifier，其次选择简单名表达式的 referencedNameElement，最后回退到当前位置叶子。
     */
    private fun findRenameNameElement(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): PsiElement? {
        val offset = document.analysisOffsetAt(position).coerceIn(0, file.textLength.coerceAtLeast(0))
        val leaf = semanticSupport.findSemanticLeaf(document, file, position) ?: return null

        leaf.parentsWithSelf().filterIsInstance<PsiNameIdentifierOwner>().forEach { owner ->
            val identifier = owner.nameIdentifier
            if (identifier != null && identifier.textRange.containsOffset(offset)) {
                return identifier
            }
        }

        leaf.parentsWithSelf().filterIsInstance<CjSimpleNameExpression>().forEach { expression ->
            val referencedNameElement = expression.referencedNameElement
            if (referencedNameElement.textRange.containsOffset(offset)) {
                return referencedNameElement
            }
        }

        return leaf.takeIf { it.textRange.containsOffset(offset) }
    }

    /**
     * prepareRename 阶段返回给客户端的目标信息。
     */
    private data class RenameTarget(
        /**
         * 客户端重命名输入框中的原始名称占位文本。
         */
        val placeholder: String,

        /**
         * 原始名称在文档中的 LSP 范围。
         */
        val range: Range,
    )

    /**
     * 从当前 PSI 元素向父节点方向遍历自身和祖先。
     */
    private fun PsiElement.parentsWithSelf(): Sequence<PsiElement> = generateSequence(this) { current ->
        current.parent as? PsiElement
    }

    /**
     * Kotlin LSP 在 EDT + write-intent/read action 中执行无 UI rename。
     *
     * 当前 LSP 是同步请求执行器，没有协程 EDT dispatcher；这里仅在 LSP 协议适配层切线程，
     * 共享 `code-insight:refactoring` 不承担宿主线程调度职责。
     */
    private fun <T> runInEdtWriteAction(action: () -> T): T {
        fun runWriteAction(): T = ApplicationManager.getApplication().runWriteAction<T> { action() }

        if (EventQueue.isDispatchThread()) return runWriteAction()

        val result = AtomicReference<T>()
        val failure = AtomicReference<Throwable>()
        EventQueue.invokeAndWait {
            try {
                result.set(runWriteAction())
            } catch (throwable: Throwable) {
                failure.set(throwable)
            }
        }

        failure.get()?.let { throw it }
        return result.get()
    }
}
