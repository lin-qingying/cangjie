package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 将重解析文本区间映射回生成该片段的真实源码区间。
 *
 * 源码 token 保留逐字符位置；由子宏替换产生的 token 映射到该子宏原始调用范围。
 * 因而 checker 在 IfAvailable 分支或实参内报告诊断时，不会被压到外层调用头。
 */
class MacroParserDiagnosticSourceMap private constructor(
    private val hostSource: CjSourceElement?,
    private val segments: List<Segment>,
    private val textLength: Int,
    private val fragmentSourceAnchor: CjSourceElement? = null,
) {
    private data class Segment(
        val fragmentStart: Int,
        val fragmentEnd: Int,
        val sourceStart: Int,
        val sourceEnd: Int,
        val exact: Boolean,
    )

    init {
        require(textLength >= 0)
        require(segments.zipWithNext().all { (left, right) -> left.fragmentEnd == right.fragmentStart }) {
            "Macro fragment source-map segments must cover one contiguous parser input."
        }
        require(segments.firstOrNull()?.fragmentStart == null || segments.first().fragmentStart == 0)
        require(segments.lastOrNull()?.fragmentEnd == null || segments.last().fragmentEnd == textLength)
    }

    /** 将 parser 输出的局部范围还原为宿主文件中的真实源码范围。 */
    fun mapRange(fragmentStart: Int, fragmentEnd: Int): CjSourceElement? {
        val source = hostSource ?: return null
        if (fragmentStart !in 0..textLength || fragmentEnd !in fragmentStart..textLength) return null
        val first = if (fragmentStart == textLength) {
            segments.lastOrNull()
        } else {
            segments.firstOrNull { fragmentStart >= it.fragmentStart && fragmentStart < it.fragmentEnd }
        } ?: return null
        val last = if (fragmentStart == fragmentEnd) {
            first
        } else {
            segments.lastOrNull { fragmentEnd > it.fragmentStart && fragmentEnd <= it.fragmentEnd }
        } ?: return null
        val start = first.mapStart(fragmentStart)
        val end = last.mapEnd(fragmentEnd)
        if (start < 0 || end < start) return null
        return CjLightSourceElement(
            lighterASTNode = source.lighterASTNode,
            startOffset = start,
            endOffset = end,
            treeStructure = source.treeStructure,
            kind = source.kind,
        )
    }

    /** 将映射限定到产生这些 CFIR 节点的重解析树。 */
    fun bindToFragmentTree(source: org.cangnova.cangjie.source.AbstractCjSourceElement?): MacroParserDiagnosticSourceMap =
        (source as? CjSourceElement)?.let { copy(fragmentSourceAnchor = it) } ?: this

    /** 判断诊断锚点是否属于本次临时片段树，包括非 CFIR 节点主 source 的子范围。 */
    fun owns(source: org.cangnova.cangjie.source.AbstractCjSourceElement): Boolean {
        val anchor = fragmentSourceAnchor ?: return false
        return when {
            anchor is CjPsiSourceElement && source is CjPsiSourceElement ->
                anchor.psi.containingFile === source.psi.containingFile
            anchor is CjLightSourceElement && source is CjLightSourceElement ->
                anchor.treeStructure === source.treeStructure
            else -> false
        }
    }

    private fun copy(fragmentSourceAnchor: CjSourceElement): MacroParserDiagnosticSourceMap =
        MacroParserDiagnosticSourceMap(hostSource, segments, textLength, fragmentSourceAnchor)

    private fun Segment.mapStart(offset: Int): Int =
        if (exact) sourceStart + (offset - fragmentStart) else sourceStart

    private fun Segment.mapEnd(offset: Int): Int =
        if (exact) sourceStart + (offset - fragmentStart) else sourceEnd

    companion object {
        /** 从重解析前的 token 来源构造映射；trimmedPrefixLength 对应 parser 输入前被剥离的空白。 */
        fun fromTokens(
            tokens: List<MacroSurfaceToken>,
            defaultSourceRange: MacroSurfaceSourceRange?,
            trimmedPrefixLength: Int,
            textLength: Int,
        ): MacroParserDiagnosticSourceMap {
            require(trimmedPrefixLength >= 0)
            require(textLength >= 0)
            val source = defaultSourceRange?.source
            val rawTextLength = trimmedPrefixLength + textLength
            var tokenStart = 0
            val segments = buildList {
                for (token in tokens) {
                    val tokenEnd = tokenStart + token.text.length
                    val clippedStart = maxOf(tokenStart, trimmedPrefixLength)
                    val clippedEnd = minOf(tokenEnd, rawTextLength)
                    if (clippedStart < clippedEnd) {
                        val sourceRange = token.sourceRange ?: defaultSourceRange
                        if (sourceRange != null) {
                            val localStart = clippedStart - tokenStart
                            val localEnd = clippedEnd - tokenStart
                            val exact = token.isSourceToken
                            add(
                                Segment(
                                    fragmentStart = clippedStart - trimmedPrefixLength,
                                    fragmentEnd = clippedEnd - trimmedPrefixLength,
                                    sourceStart = if (exact) token.startOffset + localStart else sourceRange.startOffset,
                                    sourceEnd = if (exact) token.startOffset + localEnd else sourceRange.endOffset,
                                    exact = exact,
                                ),
                            )
                        }
                    }
                    tokenStart = tokenEnd
                }
            }
            return MacroParserDiagnosticSourceMap(source, segments, textLength)
        }
    }
}
