package org.cangnova.cangjie.cfir.resolve.providers.macro

import java.util.IdentityHashMap
import java.util.Collections
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
import org.cangnova.cangjie.source.isInSameSourceTree
import org.cangnova.cangjie.source.withRange

/**
 * 对应官方 new2originPosMap 的 token 来源；输入坐标和展开文本坐标分别保存。
 * Input 保留真实输入树，Generated 保留真正产生 token 的 surface，父宏不能覆盖它。
 */
sealed class MacroTokenSourceOrigin {
    class Input(
        val source: CjSourceElement,
        val renderedText: String,
        val isLinear: Boolean,
        val linesMapping: CjSourceFileLinesMapping,
        val sourceTreeIdentity: Any = source.macroSourceTreeIdentity(),
    ) : MacroTokenSourceOrigin() {
        init {
            require(!isLinear || source.endOffset - source.startOffset == renderedText.length)
        }

        fun sourceOffset(renderedOffset: Int): Int? = when {
            renderedOffset == 0 -> source.startOffset
            renderedOffset == renderedText.length -> source.endOffset
            isLinear && renderedOffset in 0..renderedText.length -> source.startOffset + renderedOffset
            else -> null
        }
    }

    data class Generated(val surfaceIds: Set<Long>) : MacroTokenSourceOrigin() {
        constructor(surfaceId: Long) : this(setOf(surfaceId))
        init { require(surfaceIds.isNotEmpty()) }
    }
}

/**
 * 一个 token 内的来源片段。originStartOffset 属于 origin 的 renderedText，
 * 因而 lexer 合并/拆分 token 时仍能保留非线性字符串映射的两个合法端点。
 */
data class MacroTokenSourceSpan(
    val startOffset: Int,
    val endOffset: Int,
    val origin: MacroTokenSourceOrigin,
    val originStartOffset: Int = 0,
) {
    init {
        require(startOffset >= 0 && startOffset <= endOffset)
        require(originStartOffset >= 0)
        if (origin is MacroTokenSourceOrigin.Input) {
            require(originStartOffset + endOffset - startOffset <= origin.renderedText.length)
        }
    }

    fun intersect(start: Int, end: Int, destinationStart: Int): MacroTokenSourceSpan? {
        val lower = maxOf(startOffset, start)
        val upper = minOf(endOffset, end)
        if (lower >= upper) return null
        return copy(
            startOffset = destinationStart + lower - start,
            endOffset = destinationStart + upper - start,
            originStartOffset = originStartOffset + lower - startOffset,
        )
    }
}

/** 原输入 source 和生成 source 的分类，供 registry/snapshot 使用同一事实。 */
sealed class MacroCfirSourceOrigin {
    data object PreservedInput : MacroCfirSourceOrigin()
    data class Generated(val surfaceIds: Set<Long>) : MacroCfirSourceOrigin() {
        init {
            require(surfaceIds.isNotEmpty())
        }
    }
}

/**
 * raw source factory 已确认的来源。fake kind 派生 source 可以复用同一输入树身份，
 * 但生成树不能通过 offset、文件名或符号名获得原输入身份。
 */
class MacroFragmentSourceMapping(
    private val registeredOrigin: (AbstractCjSourceElement) -> MacroCfirSourceOrigin? = { null },
) {
    private val origins = IdentityHashMap<AbstractCjSourceElement, MacroCfirSourceOrigin>()
    private val originalsByDerived = IdentityHashMap<AbstractCjSourceElement, AbstractCjSourceElement>()
    private val reparsedSourcesByCreated = IdentityHashMap<AbstractCjSourceElement, MutableList<CjSourceElement>>()
    /** 原始 token 投影只读取冻结 sourceMap，供后续修饰符等子节点诊断消费。 */
    private val diagnosticProjectors = IdentityHashMap<Any, (CjSourceElement) -> CjSourceElement>()

    fun registerDiagnosticProjector(source: CjSourceElement, projector: (CjSourceElement) -> CjSourceElement) {
        diagnosticProjectors[source.macroSourceTreeIdentity()] = projector
    }

    fun diagnosticProjectors(): Map<Any, (CjSourceElement) -> CjSourceElement> =
        Collections.unmodifiableMap(IdentityHashMap(diagnosticProjectors))

    /** raw factory 实际创建关系；与 provenance 分类分开，绝不按坐标恢复 PSI 身份。 */
    fun registerCreatedSource(reparsed: CjSourceElement, created: CjSourceElement) {
        val sources = reparsedSourcesByCreated.getOrPut(created, ::mutableListOf)
        if (sources.none { it === reparsed }) sources += reparsed
    }

    fun reparsedSourcesFor(created: AbstractCjSourceElement): List<CjSourceElement> =
        reparsedSourcesByCreated[created].orEmpty().toList()

    /** 先验证来源一致，再提交；同树或可读 PSI 不构成保留输入的证明。 */
    fun register(source: AbstractCjSourceElement, origin: MacroCfirSourceOrigin) {
        val previous = originForSource(source)
        check(previous == null || previous == origin) { "A raw source cannot acquire conflicting macro provenance." }
        origins[source] = origin
    }

    /** 明确保存 fake 派生关系；原来源未知时仍保持未知，不制造 Generated/Input。 */
    fun registerDerived(original: AbstractCjSourceElement, derived: AbstractCjSourceElement) {
        if (original === derived) return
        val previous = originalsByDerived[derived]
        check(previous == null || previous === original) { "A derived source cannot acquire a different original." }
        var ancestor: AbstractCjSourceElement? = original
        while (ancestor != null) {
            check(ancestor !== derived) { "Derived source relationships must be acyclic." }
            ancestor = originalsByDerived[ancestor]
        }
        val originalOrigin = originForSource(original)
        val derivedOrigin = originForSource(derived)
        check(originalOrigin == null || derivedOrigin == null || originalOrigin == derivedOrigin) {
            "A derived source cannot acquire conflicting macro provenance."
        }
        originalsByDerived[derived] = original
    }

    fun originForSource(source: AbstractCjSourceElement): MacroCfirSourceOrigin? {
        var current: AbstractCjSourceElement? = source
        while (current != null) {
            origins[current]?.let { return it }
            registeredOrigin(current)?.let { return it }
            current = originalsByDerived[current]
        }
        return null
    }
}

/** 树身份只由 source 本身确定，不创建额外的文件路径索引。 */
fun CjSourceElement.macroSourceTreeIdentity(): Any = when (this) {
    is CjPsiSourceElement -> checkNotNull(psi.containingFile) { "A macro token input PSI must have a source tree." }
    is CjLightSourceElement -> unwrapToCjPsiSourceElement()?.psi?.containingFile ?: treeStructure
    is CjBinarySourceElement -> error("Binary sources cannot be macro token origins.")
}

/**
 * 稳定展开文本及其分段来源。token 范围属于 text；source 范围属于原输入树。
 * 空格重建、trim 和 parser wrapper 均通过显式区间变换处理。
 */
class MacroFragmentSourceMap private constructor(
    val text: String,
    private val tokens: List<TokenRange>,
    private val spans: List<MacroTokenSourceSpan>,
) {
    private data class TokenRange(val start: Int, val end: Int, val trivia: Boolean)
    private data class OriginRange(val start: Int, val end: Int)
    private val occurrences = IdentityHashMap<Any, MutableMap<OriginRange, MutableSet<Int>>>()

    init {
        var previous: MacroTokenSourceSpan? = null
        for (span in spans) {
            val input = span.origin as? MacroTokenSourceOrigin.Input ?: continue
            val continues = previous?.let {
                it.origin === input && it.endOffset == span.startOffset &&
                    it.originStartOffset + it.endOffset - it.startOffset == span.originStartOffset
            } == true
            if (!continues) {
                occurrences.getOrPut(input.sourceTreeIdentity) { mutableMapOf() }
                    .getOrPut(OriginRange(input.source.startOffset, input.source.endOffset)) { mutableSetOf() }
                    .add(span.startOffset - span.originStartOffset)
            }
            previous = span
        }
    }

    /** 复扫只切分来源片段；不能重新赋予原始文件 offset。 */
    fun spansForRange(start: Int, end: Int): List<MacroTokenSourceSpan> {
        require(start in 0..text.length && end in start..text.length)
        return spansInRange(start, end).mapNotNull { it.intersect(start, end, destinationStart = 0) }
    }

    fun trimmed(): MacroFragmentSourceMap {
        val start = text.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: text.length
        val end = if (start == text.length) start else text.indexOfLast { !it.isWhitespace() } + 1
        return MacroFragmentSourceMap(
            text.substring(start, end),
            tokens.mapNotNull { token ->
                val lower = maxOf(start, token.start)
                val upper = minOf(end, token.end)
                if (lower >= upper) null else TokenRange(lower - start, upper - start, token.trivia)
            },
            spansForRange(start, end),
        )
    }

    /** 只有完整、同树、单调且唯一的输入来源可以成为规范源码锚点。 */
    fun originalRange(
        start: Int, end: Int, requireUnique: Boolean = true, includeTrivia: Boolean = false,
    ): CjSourceElement? {
        if (start !in 0..text.length || end !in start..text.length || start == end) return null
        val selected = mutableListOf<MacroTokenSourceSpan>()
        for (token in tokensInRange(start, end)) {
            if ((token.trivia && !includeTrivia) || token.end <= start || token.start >= end) continue
            val lower = maxOf(start, token.start)
            val upper = minOf(end, token.end)
            val pieces = spansInRange(lower, upper).mapNotNull { it.intersect(lower, upper, destinationStart = lower) }
            var cursor = lower
            for (piece in pieces) {
                if (piece.startOffset != cursor || piece.origin !is MacroTokenSourceOrigin.Input) return null
                selected += piece
                cursor = piece.endOffset
            }
            if (cursor != upper) return null
        }
        if (selected.isEmpty()) return null

        val combined = mutableListOf<MacroTokenSourceSpan>()
        for (piece in selected) {
            val previous = combined.lastOrNull()
            if (previous != null && previous.origin === piece.origin &&
                previous.endOffset == piece.startOffset &&
                previous.originStartOffset + previous.endOffset - previous.startOffset == piece.originStartOffset
            ) {
                combined[combined.lastIndex] = previous.copy(endOffset = piece.endOffset)
            } else {
                combined += piece
            }
        }

        var source: CjSourceElement? = null
        var sourceTreeIdentity: Any? = null
        var originalStart = -1
        var originalEnd = -1
        for (piece in combined) {
            val input = piece.origin as MacroTokenSourceOrigin.Input
            if (requireUnique) {
                val count = occurrences[input.sourceTreeIdentity]
                    ?.get(OriginRange(input.source.startOffset, input.source.endOffset))?.size ?: 0
                if (count != 1) return null
            }
            val lower = input.sourceOffset(piece.originStartOffset) ?: return null
            val upper = input.sourceOffset(piece.originStartOffset + piece.endOffset - piece.startOffset) ?: return null
            if (source == null) {
                source = input.source
                sourceTreeIdentity = input.sourceTreeIdentity
                originalStart = lower
            } else if (sourceTreeIdentity !== input.sourceTreeIdentity || lower < originalEnd) {
                return null
            }
            originalEnd = upper
        }
        return checkNotNull(source).withRange(originalStart, originalEnd)
    }

    /**
     * 来源事实独立于 canonical 投影。只要求非 trivia 范围被来源片段完整覆盖；
     * 不检查 Input 的树一致性、唯一性、单调性或非线性内部端点。
     * 混合范围保留实际 Generated producer；无 token/缺证据返回 null。
     */
    fun originForRange(start: Int, end: Int): MacroCfirSourceOrigin? {
        if (start !in 0..text.length || end !in start..text.length || start == end) return null
        var hasInput = false
        var hasTokens = false
        val generated = linkedSetOf<Long>()
        for (token in tokensInRange(start, end)) {
            if (token.trivia) continue
            hasTokens = true
            val lower = maxOf(start, token.start)
            val upper = minOf(end, token.end)
            var cursor = lower
            for (piece in spansInRange(lower, upper)) {
                val clipped = piece.intersect(lower, upper, destinationStart = lower) ?: continue
                if (clipped.startOffset != cursor) return null
                when (val origin = clipped.origin) {
                    is MacroTokenSourceOrigin.Input -> hasInput = true
                    is MacroTokenSourceOrigin.Generated -> generated.addAll(origin.surfaceIds)
                }
                cursor = clipped.endOffset
            }
            if (cursor != upper) return null
        }
        return when {
            generated.isNotEmpty() -> MacroCfirSourceOrigin.Generated(generated)
            hasTokens && hasInput -> MacroCfirSourceOrigin.PreservedInput
            else -> null
        }
    }

    private fun spansInRange(start: Int, end: Int): List<MacroTokenSourceSpan> {
        var lower = 0
        var upper = spans.size
        while (lower < upper) {
            val middle = (lower + upper) ushr 1
            if (spans[middle].endOffset <= start) lower = middle + 1 else upper = middle
        }
        val first = lower
        while (lower < spans.size && spans[lower].startOffset < end) lower++
        return spans.subList(first, lower)
    }

    private fun tokensInRange(start: Int, end: Int): List<TokenRange> {
        var lower = 0
        var upper = tokens.size
        while (lower < upper) {
            val middle = (lower + upper) ushr 1
            if (tokens[middle].end <= start) lower = middle + 1 else upper = middle
        }
        val first = lower
        while (lower < tokens.size && tokens[lower].start < end) lower++
        return tokens.subList(first, lower)
    }

    companion object {
        fun materialize(tokens: List<MacroSurfaceToken>, currentSurfaceId: Long? = null): MacroFragmentSourceMap {
            val text = StringBuilder()
            val ranges = mutableListOf<TokenRange>()
            val spans = mutableListOf<MacroTokenSourceSpan>()
            for (token in tokens) {
                val start = text.length
                text.append(token.text)
                val end = text.length
                if (start == end) continue
                ranges += TokenRange(start, end, token.isTrivia)
                val tokenSpans = token.sourceSpans.ifEmpty {
                    if (currentSurfaceId == null) emptyList() else listOf(
                        MacroTokenSourceSpan(0, token.text.length, MacroTokenSourceOrigin.Generated(currentSurfaceId)),
                    )
                }
                var cursor = 0
                for (span in tokenSpans) {
                    require(span.startOffset >= cursor && span.endOffset <= token.text.length)
                    if (cursor < span.startOffset && currentSurfaceId != null) {
                        spans += MacroTokenSourceSpan(
                            start + cursor, start + span.startOffset, MacroTokenSourceOrigin.Generated(currentSurfaceId),
                        )
                    }
                    spans += span.copy(startOffset = start + span.startOffset, endOffset = start + span.endOffset)
                    cursor = span.endOffset
                }
                if (cursor < token.text.length && currentSurfaceId != null) {
                    spans += MacroTokenSourceSpan(
                        start + cursor, end, MacroTokenSourceOrigin.Generated(currentSurfaceId),
                    )
                }
            }
            return MacroFragmentSourceMap(text.toString(), ranges, spans)
        }
    }
}
