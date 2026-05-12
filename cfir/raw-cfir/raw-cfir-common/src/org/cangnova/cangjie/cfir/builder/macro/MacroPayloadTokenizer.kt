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
}
