package org.cangnova.cangjie.frontend.pipeline

import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentSourceMap
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceSourceRange
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroTokenSourceOrigin
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroTokenSourceSpan
import org.cangnova.cangjie.cfir.resolve.providers.macro.macroSourceTreeIdentity
import org.cangnova.cangjie.macro.SourcePosition
import org.cangnova.cangjie.macro.TokenInfo
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
import org.cangnova.cangjie.source.toSourceLinesMapping

/**
 * 一次 executor 调用的 wire token 来源表。key 包含完整 TokenInfo（kind/value/位置/定界符），
 * 不按 token 名字匹配。树到 file_id 的表只服务协议，不是 PSI owning-file/provider 索引。
 */
internal class MacroProtocolSourceMapping(
    rootSource: CjSourceElement?,
    private val surfaceId: Long,
) {
    private data class TokenInput(val text: String, val spans: List<MacroTokenSourceSpan>)
    private val inputs = mutableMapOf<TokenInfo, MutableList<TokenInput>>()
    private val sourceIds = IdentityHashMap<Any, Int>()
    private var nextFileId = 1
    private val rootIdentity = rootSource?.macroSourceTreeIdentity()
    private val rootLines: CjSourceFileLinesMapping? = rootSource?.let { source ->
        val text = when (source) {
            is CjPsiSourceElement -> checkNotNull(source.psi.containingFile).text
            is CjLightSourceElement -> source.treeStructure.toString(source.treeStructure.root)
            else -> error("A macro call requires a text source.")
        }
        text.toSourceLinesMapping()
    }

    val rootFileId: Int = rootIdentity?.let(::fileId) ?: 0

    private fun fileId(identity: Any): Int = sourceIds.getOrPut(identity) { nextFileId++ }

    fun rootPosition(offset: Int?): SourcePosition {
        val lines = rootLines ?: return SourcePosition()
        if (offset == null || offset !in 0..lines.lastOffset) return SourcePosition(fileId = rootFileId)
        val (line, column) = lines.getLineAndColumnByOffset(offset)
        return SourcePosition(rootFileId, line + 1, column + 1)
    }

    /** A generated child token uses its call's wire coordinates without acquiring input provenance. */
    fun childCallPositions(range: MacroSurfaceSourceRange): Pair<SourcePosition, SourcePosition>? {
        val lines = rootLines ?: return null
        val source = range.source ?: return null
        if (source.macroSourceTreeIdentity() !== rootIdentity) return null
        if (range.startOffset !in 0..lines.lastOffset || range.endOffset !in range.startOffset..lines.lastOffset) return null
        return rootPosition(range.startOffset) to rootPosition(range.endOffset)
    }

    fun inputPosition(input: MacroTokenSourceOrigin.Input, offset: Int): SourcePosition {
        require(offset in 0..input.linesMapping.lastOffset)
        val (line, column) = input.linesMapping.getLineAndColumnByOffset(offset)
        return SourcePosition(fileId(input.sourceTreeIdentity), line + 1, column + 1)
    }

    fun record(token: TokenInfo, text: String, spans: List<MacroTokenSourceSpan>) {
        val candidate = TokenInput(text, spans)
        val candidates = inputs.getOrPut(token) { mutableListOf() }
        if (candidates.none { sameInput(it, candidate) }) candidates += candidate
    }

    /** 只保留唯一的 wire 来源；规范化字面量只提供端点映射，不假设字符线性对应。 */
    fun spansForOutput(token: TokenInfo, renderedText: String): List<MacroTokenSourceSpan> {
        if (renderedText.isEmpty()) return emptyList()
        val candidates = inputs[token].orEmpty()
        if (candidates.size != 1) return generatedSpans(renderedText, candidates)
        val input = candidates.single()
        if (input.text == renderedText) return input.spans.ifEmpty { generatedSpans(renderedText, candidates) }
        val sourceMap = MacroFragmentSourceMap.materialize(
            listOf(MacroSurfaceToken(input.text, 0, input.text.length, sourceSpans = input.spans)),
        )
        val original = sourceMap.originalRange(
            0, input.text.length, requireUnique = false, includeTrivia = true,
        ) ?: return generatedSpans(renderedText, candidates)
        val first = input.spans.firstOrNull()?.origin as? MacroTokenSourceOrigin.Input
            ?: return generatedSpans(renderedText, candidates)
        val origin = MacroTokenSourceOrigin.Input(
            source = original,
            renderedText = renderedText,
            isLinear = false,
            linesMapping = first.linesMapping,
            sourceTreeIdentity = first.sourceTreeIdentity,
        )
        return listOf(MacroTokenSourceSpan(0, renderedText.length, origin))
    }

    private fun generatedSpans(text: String, candidates: List<TokenInput>): List<MacroTokenSourceSpan> {
        val generated = candidates.flatMap { candidate ->
            candidate.spans.flatMap { span ->
                (span.origin as? MacroTokenSourceOrigin.Generated)?.surfaceIds.orEmpty()
            }
        }.toSet()
        val origin = MacroTokenSourceOrigin.Generated(if (generated.isEmpty()) setOf(surfaceId) else generated)
        return listOf(MacroTokenSourceSpan(0, text.length, origin))
    }

    private fun sameInput(first: TokenInput, second: TokenInput): Boolean {
        if (first.text != second.text || first.spans.size != second.spans.size) return false
        return first.spans.zip(second.spans).all { (left, right) ->
            if (left.startOffset != right.startOffset || left.endOffset != right.endOffset ||
                left.originStartOffset != right.originStartOffset
            ) return@all false
            val a = left.origin
            val b = right.origin
            when {
                a is MacroTokenSourceOrigin.Input && b is MacroTokenSourceOrigin.Input ->
                    a.sourceTreeIdentity === b.sourceTreeIdentity &&
                        a.source.startOffset == b.source.startOffset && a.source.endOffset == b.source.endOffset &&
                        a.renderedText == b.renderedText && a.isLinear == b.isLinear
                a is MacroTokenSourceOrigin.Generated && b is MacroTokenSourceOrigin.Generated ->
                    a.surfaceIds == b.surfaceIds
                else -> false
            }
        }
    }
}
