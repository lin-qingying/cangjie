package org.cangnova.cangjie.codeinsight.hints

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjQualifiedExpression
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType

/**
 * 链式调用类型提示收集器。
 *
 * 多行链式调用里，中间每一步的中间类型都不写在源码上，读的时候只能靠心算。
 * 这里在**每个换行的调用环节之后**标出该环节的结果类型，形如：
 * ```
 * list.filter { ... }  // : List<Int64>
 *     .map { ... }     // : List<Int64>
 *     .takeIf { ... }  // : Option<List<Int64>>
 * ```
 *
 * 只处理**多行**链（至少一个 `.` / `?.` 另起一行）：单行链本身一眼能读完，
 * 再加提示只是噪声。`Unit` 结尾（`forEach`）与错误类型不产出。
 */
internal object CjCallChainHintsCollector {
    /**
     * 类型文本渲染策略：源码视角必须用短名。
     */
    private val TYPE_RENDERER = CaTypeRendererForSource.WITH_SHORT_NAMES

    /**
     * 收集 `file` 内（可限定到 `range`）的链式调用类型提示。
     */
    fun collect(
        file: CjFile,
        range: TextRange?,
        session: CaSession,
    ): List<CangJieInlayHint> =
        candidates(file, range).mapNotNull { candidate -> hintFor(file, candidate, session) }

    /**
     * 挑选所有多行链上的调用环节。
     *
     * 纯 PSI 逻辑：链的判定、换行判定与 range 过滤都不做类型解析。
     */
    fun candidates(
        file: CjFile,
        range: TextRange?,
    ): List<CjCallChainHintCandidate> =
        file.collectDescendantsOfType<CjExpression>()
            .filterIsInstance<CjQualifiedExpression>()
            .filter { link -> isChainRoot(link) }
            .flatMap { root -> linksOf(root).mapNotNull { link -> candidate(link, range) } }
            // 链按「由外到内」拆出，这里还原成源码顺序，便于测试与后续稳定排序。
            .sortedBy { it.offset }

    /**
     * 是否为一条链的根：父节点不是限定表达式，或者父节点的接收者不是自己。
     *
     * 链内部每个环节都会被 `collectDescendantsOfType` 收集到，只有根能代表整条链，
     * 否则同一条链会被反复处理。
     */
    private fun isChainRoot(link: CjQualifiedExpression): Boolean {
        val parent = (link as CjExpression).parent as? CjQualifiedExpression ?: return true
        return parent.receiverExpression !== link
    }

    /**
     * 沿接收者方向拆出一整条链，顺序为「由外到内」。
     */
    private fun linksOf(root: CjQualifiedExpression): List<CjQualifiedExpression> {
        val links = ArrayList<CjQualifiedExpression>()
        var current: CjExpression? = root
        while (current is CjQualifiedExpression) {
            links += current
            current = current.receiverExpression
        }
        // 只有一格（`a.b()`）不算链；且必须真的换行才有提示价值。
        if (links.size < MIN_CHAIN_LENGTH) return emptyList()
        if (links.none { isMultilineLink(it) }) return emptyList()
        return links.filter { isMultilineLink(it) }
    }

    /**
     * 环节是否另起一行：`. ` 所在行与接收者起始行不同。
     */
    private fun isMultilineLink(link: CjQualifiedExpression): Boolean {
        val element = link as CjExpression
        val receiver = link.receiverExpression
        val document = element.containingFile.viewProvider.document ?: return false
        val startLine = document.getLineNumber(receiver.textRange.startOffset)
        val operatorLine = document.getLineNumber(link.operationTokenNode.startOffset)
        return operatorLine > startLine
    }

    /**
     * 单个环节的候选判定：只提示调用（`a.b()`），纯属性访问不标。
     */
    private fun candidate(
        link: CjQualifiedExpression,
        range: TextRange?,
    ): CjCallChainHintCandidate? {
        if (link.selectorExpression !is CjCallExpression) return null
        if (CjInlayHintFilters.hasErrorAncestor(link as CjExpression)) return null

        val offset = (link as CjExpression).textRange.endOffset
        if (!CjInlayHintFilters.isInRange(offset, range)) return null
        return CjCallChainHintCandidate(link, offset)
    }

    /**
     * 把单个候选解析成提示；任何失败都只丢这一条。
     */
    private fun hintFor(
        file: CjFile,
        candidate: CjCallChainHintCandidate,
        session: CaSession,
    ): CangJieInlayHint? = try {
        resolveHint(file, candidate, session)
    } catch (cancelled: ProcessCanceledException) {
        // 取消必须原样上抛：它不是「这条提示算不出来」，而是整个请求不要了。
        throw cancelled
    } catch (failure: Throwable) {
        LOG.warn("仓颉 inlay hint（链式调用类型）解析失败", failure)
        null
    }

    /**
     * 在 session 内解析环节类型并渲染成纯文本。
     */
    private fun resolveHint(
        file: CjFile,
        candidate: CjCallChainHintCandidate,
        session: CaSession,
    ): CangJieInlayHint? {
        val element = candidate.link as CjExpression
        if (CjInlayHintFilters.isInsideMacroExpansion(file, element.textRange.startOffset, session)) {
            return null
        }

        val type = with(session) { element.expressionType } ?: return null
        if (with(session) { type.isErrorType }) return null

        val rendered = with(session) { type.render(TYPE_RENDERER) }.trim()
        if (rendered.isEmpty()) return null
        // `forEach { ... }` 这类链尾返回 Unit，标出来没有信息量。
        if (rendered == UNIT_TYPE_NAME) return null

        return CangJieInlayHint(
            offset = candidate.offset,
            kind = CangJieInlayHintKind.TYPE,
            target = CangJieInlayHintTarget.CALL_CHAIN_TYPE,
            label = listOf(CangJieInlayHintLabelPart(": $rendered")),
        )
    }

    /**
     * 链的最短长度：`a.b()` 这种一格的不算链。
     */
    private const val MIN_CHAIN_LENGTH = 2

    /**
     * 单元类型名：与其它类型提示保持同一套噪声过滤。
     */
    private const val UNIT_TYPE_NAME = "Unit"

    private val LOG: Logger = Logger.getInstance(CjCallChainHintsCollector::class.java)
}

/**
 * 链式调用提示的候选：链上的一个调用环节。
 *
 * 只携带 PSI 锚点，不含任何 session 生命周期对象。
 */
internal data class CjCallChainHintCandidate(
    /** 链上承载该提示的环节。 */
    val link: CjQualifiedExpression,
    /** 提示插入位置（环节末尾）。 */
    val offset: Int,
)
