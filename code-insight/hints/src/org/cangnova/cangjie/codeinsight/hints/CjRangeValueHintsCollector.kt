package org.cangnova.cangjie.codeinsight.hints

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjRangeExpressionBase
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType

/**
 * 区间开闭性提示收集器。
 *
 * `0..10` 与 `0..=10` 只差一个 `=`，读代码时很容易看漏终点是否包含。这里在端点旁各插一个
 * 关系符，把开闭性写成人人可读的不等式：`0 ≤ .. < 10` / `0 ≤ .. ≤ 10`。
 *
 * 为什么不用区间记法 `[0, 10)`：切片 `a[0..10]` 本身已带方括号，再补方括号会渲染成
 * `a[[0..10)]`。不等式形式与宿主上下文无关，切片与普通范围可以共用同一套文本。
 *
 * 该提示只依赖 PSI（端点位置 + `..`/`..=`），不需要类型信息；`session` 仅用于宏展开过滤。
 */
internal object CjRangeValueHintsCollector {
    /**
     * 收集 `file` 内（可限定到 `range`）的区间开闭性提示。
     */
    fun collect(
        file: CjFile,
        range: TextRange?,
        session: CaSession,
    ): List<CangJieInlayHint> =
        candidates(file, range).flatMap { candidate ->
            hintsFor(file, candidate, session)
        }

    /**
     * 挑选所有带端点的范围/切片表达式。
     *
     * 纯 PSI 逻辑：端点缺失（开区间切片 `a[..10]` / `a[0..]`）时只提示存在的那一端。
     */
    fun candidates(
        file: CjFile,
        range: TextRange?,
    ): List<CjRangeValueHintCandidate> =
        file.collectDescendantsOfType<CjRangeExpressionBase>().mapNotNull { expression ->
            candidate(expression, range)
        }

    /**
     * 单个表达式的候选判定。
     */
    private fun candidate(
        expression: CjRangeExpressionBase,
        range: TextRange?,
    ): CjRangeValueHintCandidate? {
        // 先判错误：`rangeOperatorNode` 在残缺节点上会直接抛错，不能先碰运算符。
        if (CjInlayHintFilters.hasErrorAncestor(expression)) return null

        val markers = ArrayList<CjRangeValueHintMarker>(2)
        expression.startExpression?.let { start ->
            // 起点恒为闭（`..` 与 `..=` 都包含起点），所以左端一律是 `≤`。
            val offset = start.textRange.endOffset
            if (CjInlayHintFilters.isInRange(offset, range)) {
                markers += CjRangeValueHintMarker(offset, INCLUSIVE_MARKER)
            }
        }
        expression.stopExpression?.let { stop ->
            val offset = stop.textRange.startOffset
            if (CjInlayHintFilters.isInRange(offset, range)) {
                val marker = if (expression.isInclusive) INCLUSIVE_MARKER else EXCLUSIVE_MARKER
                markers += CjRangeValueHintMarker(offset, marker)
            }
        }
        if (markers.isEmpty()) return null
        return CjRangeValueHintCandidate(expression, markers)
    }

    /**
     * 产出候选对应的提示；任何失败都只丢这一个表达式。
     */
    private fun hintsFor(
        file: CjFile,
        candidate: CjRangeValueHintCandidate,
        session: CaSession,
    ): List<CangJieInlayHint> {
        // 宏展开区间内的 PSI 是展开产物，源码视角的提示会指向用户没写的代码。
        if (CjInlayHintFilters.isInsideMacroExpansion(file, candidate.expression.textRange.startOffset, session)) {
            return emptyList()
        }
        return candidate.markers.map { marker ->
            CangJieInlayHint(
                offset = marker.offset,
                kind = CangJieInlayHintKind.OTHER,
                target = CangJieInlayHintTarget.RANGE_VALUE,
                label = listOf(CangJieInlayHintLabelPart(marker.text)),
            )
        }
    }

    /**
     * 端点关系符：包含用 `≤`，不包含用 `<`。
     *
     * 文本自带两侧空格：IDE 的声明式 sink 不支持 padding，把空格写进标签才能让两个宿主
     * 渲染出同样的 `0 ≤ .. < 10`。
     */
    private const val INCLUSIVE_MARKER = " ≤ "

    /**
     * 不含终点时的关系符。
     */
    private const val EXCLUSIVE_MARKER = " < "
}

/**
 * 区间开闭性提示的候选：一个范围表达式 + 它的端点标记。
 *
 * 只携带 PSI 与已渲染文本，不含任何 session 生命周期对象。
 */
internal data class CjRangeValueHintCandidate(
    /** 承载端点的范围/切片表达式。 */
    val expression: CjRangeExpressionBase,
    /** 端点标记，按源码顺序排列。 */
    val markers: List<CjRangeValueHintMarker>,
)

/**
 * 单个端点的提示位置与文本。
 */
internal data class CjRangeValueHintMarker(
    /** 提示插入位置。 */
    val offset: Int,
    /** 已渲染的关系符文本。 */
    val text: String,
)
