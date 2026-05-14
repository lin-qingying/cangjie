package org.cangnova.cangjie.cfir.builder.macro

import org.cangnova.cangjie.lexer.CangJieLexer

/**
 * Raw builder 层中立 token。
 *
 * 该类型不依赖 providers 层，调用方在 PSI/LightTree/frontend 装配边界
 * 显式映射为 construction core 的 `MacroSurfaceToken`。
 */
data class MacroPayloadToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val kindName: String? = null,
)

/**
 * Macro construction 期 attr / input payload token 拆词工具。
 *
 * Baseline Batch 6 入口 —— 用真实 [CangJieLexer] 对 payload 字符串重新分词，
 * 替换 4b 阶段 "整段字符串当一条 token" 的占位实现。
 *
 * `useParentPos` / quote / string interpolation 的特殊语义将在
 * Batch 7 (forest evaluator) 与 Batch 8 (fragment parser) 引入；本入口
 * 只承担 "把一段 payload 文本拆成 lexer token 流" 这一职责。
 *
 * 不在此处处理：
 * - macro call 内部递归调用（Batch 7 macro forest）
 * - quote 中字符串插值变量取值（Batch 8 fragment parser）
 */
object MacroPayloadTokenizer {

    /**
     * Token scanner 算法版本。PLAN.md §11 cache key 第 11 维。
     *
     * 任何会改变 token 拆分结果（lexer 规则、whitespace 处理、kind 命名）
     * 的修改都必须递增此值，从而触发上游 macro cache 失效。
     */
    const val VERSION: Int = 1

    /**
     * 把 [payload] 拆为 token 流。
     *
     * 拆词过程对 lexer 的 whitespace / comment token 一并保留，
     * 因为 fragment parser 后续需要据此重组 source 形态。
     *
     * @param payload 待拆词的字符串；可能为空（返回空列表）。
     * @param baseOffset payload 在宿主源文件中的起始偏移。每条 token 的
     *                   `startOffset` / `endOffset` 都会加上这个基准，
     *                   使其直接对齐宿主源文件坐标。
     */
    fun tokenize(payload: CharSequence?, baseOffset: Int = 0): List<MacroPayloadToken> {
        if (payload.isNullOrEmpty()) return emptyList()
        val lexer = CangJieLexer()
        lexer.start(payload, 0, payload.length, 0)
        val tokens = mutableListOf<MacroPayloadToken>()
        while (lexer.tokenType != null) {
            val start = lexer.tokenStart
            val end = lexer.tokenEnd
            val text = payload.substring(start, end)
            val kind = lexer.tokenType?.toString()
            tokens += MacroPayloadToken(
                text = text,
                startOffset = baseOffset + start,
                endOffset = baseOffset + end,
                kindName = kind,
            )
            lexer.advance()
        }
        return tokens
    }

    /**
     * 对 macro executor 返回的 newTokens 做真实 lexer 复扫。
     *
     * providers 层的 `TokenBackedMacroFragmentParser` 只能接收函数注入；
     * raw-cfir-common 作为拥有仓颉 lexer 的层，提供唯一生产级复扫入口。
     */
    fun reTokenize(tokens: List<MacroPayloadToken>): List<MacroPayloadToken> {
        val text = tokens.joinToString(separator = "") { it.text }
        return tokenize(text, baseOffset = 0)
    }

    /**
     * 把 [tokens] 反复 [reTokenize] 直至序列稳定或达到 [maxIterations]。
     *
     * PLAN.md §8 要求 "newTokens 先 token-stage re-eval 到 stable，再 fragment parse"。
     * 单次 [reTokenize] 已能在大部分情况下达到稳定（因为输入文本与拆分都是确定的），
     * 但当 executor 输出的 token 流粘连（如未分隔的标识符与数字）时，
     * 第二次 lex 才能给出最终边界。固定点迭代保证 fragment parser 上游始终拿到稳定输入。
     *
     * 当达到 [maxIterations] 仍未稳定时返回最后一次结果，调用方按 stable 处理；
     * 这是为了避免病态输入下死循环。
     */
    fun reTokenizeUntilStable(
        tokens: List<MacroPayloadToken>,
        maxIterations: Int = 4,
    ): List<MacroPayloadToken> {
        require(maxIterations >= 1) { "maxIterations must be >= 1, got $maxIterations" }
        var current = tokens
        repeat(maxIterations) {
            val next = reTokenize(current)
            if (next.sameSequence(current)) return next
            current = next
        }
        return current
    }

    private fun List<MacroPayloadToken>.sameSequence(other: List<MacroPayloadToken>): Boolean {
        if (size != other.size) return false
        for (i in indices) {
            val a = this[i]
            val b = other[i]
            if (a.text != b.text || a.kindName != b.kindName) return false
        }
        return true
    }
}
