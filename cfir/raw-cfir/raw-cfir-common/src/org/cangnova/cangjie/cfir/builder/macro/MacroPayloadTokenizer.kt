package org.cangnova.cangjie.cfir.builder.macro

import org.cangnova.cangjie.lexer.CangJieLexer

/**
 * Raw builder 层中立 token。
 *
 * 该类型不依赖 providers 层，调用方在 PSI/LightTree/frontend 装配边界
 * 显式映射为 construction core 的 `MacroSurfaceToken`。
 *
 * @property text token 原始文本。
 * @property startOffset token 在宿主源文件中的起始偏移。
 * @property endOffset token 在宿主源文件中的结束偏移。
 * @property kindName lexer token type 字符串名。
 */
data class MacroPayloadToken(
    /** token 原始文本。 */
    val text: String,
    /** token 在宿主源文件中的起始偏移。 */
    val startOffset: Int,
    /** token 在宿主源文件中的结束偏移。 */
    val endOffset: Int,
    /** lexer token type 字符串名。 */
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
     *
     * v2：把 lexer 拆分出的字符串片段合并为 CJC 的整段字符串 token。
     */
    const val VERSION: Int = 2

    private const val OPEN_QUOTE_KIND = "OPEN_QUOTE"
    private const val CLOSING_QUOTE_KIND = "CLOSING_QUOTE"
    private const val ILLEGAL_KIND = "ILLEGAL"

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

            // CJC 宏协议没有 `<:`（LT_COLON）这一 TokenKind；官方词法把它视为 `<` 与 `:` 两个 token。
            // 这里按同样的方式拆分，避免直接失败。
            if (kind == "LT_COLON" && text == "<:") {
                tokens += MacroPayloadToken("<", baseOffset + start, baseOffset + start + 1, "LT")
                tokens += MacroPayloadToken(":", baseOffset + start + 1, baseOffset + end, "COLON")
                lexer.advance()
                continue
            }

            tokens += MacroPayloadToken(
                text = text,
                startOffset = baseOffset + start,
                endOffset = baseOffset + end,
                kindName = kind,
            )
            lexer.advance()
        }
        return mergeStringLiterals(tokens)
    }

    /**
     * 把 IntelliJ lexer 的字符串片段合并成 CJC 的整段字符串 token。
     *
     * IntelliJ lexer 把 `"..."` 拆成 `OPEN_QUOTE` / 内容 / `CLOSING_QUOTE`（插值内部还可能嵌套字符串），
     * 而 CJC 把整段字符串（含 `${...}` 插值）产出一个 `STRING_LITERAL` / `MULTILINE_STRING` /
     * `MULTILINE_RAW_STRING` token。宏协议必须发送后者，否则 `MacroTokenKind.fromLexerToken`
     * 找不到 `OPEN_QUOTE` 的映射而直接失败。
     */
    private fun mergeStringLiterals(tokens: List<MacroPayloadToken>): List<MacroPayloadToken> {
        if (tokens.none { it.kindName == OPEN_QUOTE_KIND }) return tokens

        val merged = ArrayList<MacroPayloadToken>(tokens.size)
        var index = 0
        while (index < tokens.size) {
            val token = tokens[index]
            if (token.kindName != OPEN_QUOTE_KIND) {
                merged += token
                index++
                continue
            }

            var depth = 0
            var closingIndex = -1
            for (probe in index until tokens.size) {
                when (tokens[probe].kindName) {
                    OPEN_QUOTE_KIND -> depth++
                    CLOSING_QUOTE_KIND -> {
                        depth--
                        if (depth == 0) {
                            closingIndex = probe
                            break
                        }
                    }
                }
            }

            if (closingIndex < 0) {
                // 未闭合字符串：整体收敛为单个非法 token（与 CJC 的非法 token 语义一致），
                // 避免残留 OPEN_QUOTE/REGULAR_STRING_PART 让下游因无映射而中断分析。
                val text = buildString {
                    for (part in index until tokens.size) {
                        append(tokens[part].text)
                    }
                }
                merged += MacroPayloadToken(
                    text = text,
                    startOffset = token.startOffset,
                    endOffset = tokens.last().endOffset,
                    kindName = ILLEGAL_KIND,
                )
                break
            }

            val text = buildString {
                for (part in index..closingIndex) {
                    append(tokens[part].text)
                }
            }
            merged += MacroPayloadToken(
                text = text,
                startOffset = token.startOffset,
                endOffset = tokens[closingIndex].endOffset,
                kindName = cjcStringKindName(token.text),
            )
            index = closingIndex + 1
        }
        return merged
    }

    /** 依据开头引号形态选择 CJC 的字符串 token kind。 */
    private fun cjcStringKindName(openingQuote: String): String = when {
        openingQuote.contains('#') -> "MULTILINE_RAW_STRING"
        openingQuote.contains("\"\"\"") || openingQuote.contains("'''") -> "MULTILINE_STRING"
        else -> "STRING_LITERAL"
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

    /** 比较两个 token 序列的文本和 kind 是否完全一致。 */
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
