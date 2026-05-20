package org.cangnova.cangjie.cfir.resolve.providers.macro

/**
 * Macro fragment parser 入口（baseline 第 12 节 Batch 8）。
 *
 * 它在 macro construction 期承担"把 macro evaluator 输出的 token 流
 * 重新解析为 CFIR 片段（fragment）"这一职责。Fragment parser 是
 * **construction-only** 的：它的产物不进入 generated cfir-tree visitor。
 *
 * 解析模式参考 baseline 第 8 节"custom annotation only has two routes：
 * `ResolveMacroCall -> ReclassifiedAnnotation` / fragment parser
 * `custom-annotation mode`"。
 */
interface MacroFragmentParser {
    /**
     * 解析 macro fragment。
     *
     * [MacroFragmentInput] 显式携带 final routing decision 和 annotation slot
     * snapshot；CUSTOM_ANNOTATION 禁止只用裸 token 推断输入。
     */
    fun parse(input: MacroFragmentInput): MacroFragmentResult

    enum class Mode { EXPRESSION, DECLARATION, CUSTOM_ANNOTATION }

    companion object {
        /**
         * Fragment parser 算法版本。PLAN.md §11 cache key 第 12 维。
         *
         * 解析入口、custom-annotation fallback、payload 类型语义有变化时递增。
         */
        const val VERSION: Int = 1
    }
}

/**
 * fragment parser 的完整输入。
 */
data class MacroFragmentInput(
    val node: MacroCallNode,
    val tokens: List<MacroSurfaceToken>,
    val decision: FinalMacroSurfaceDecision,
    val annotationSnapshot: CfirAnnotationSlotSnapshot? = null,
) {
    val mode: MacroFragmentParser.Mode
        get() = decision.parserMode
}

/**
 * Construction-only 的 fragment 结果。
 *
 * Baseline 第 2 节硬性边界 #6 / 第 8 节：fragment parser 输出
 * **不**新增 final 泛型 `CfirNode`。本 sealed type 只承载 construction
 * 内部数据，splice 阶段会用它的内容生成最终 CFIR 节点（由 splice 实现
 * 而不是 fragment parser）。
 *
 * - [Success] 携带原始 token 与诊断；
 * - [Failure] 携带失败原因（CLI strict 会推升至 Failed/Degraded）；
 * - [CustomAnnotation] 用于 fallback：fragment 内容应当被 reclassify
 *   为 annotation，而不是 macro call。
 */
sealed class MacroFragmentResult {
    abstract val originNode: MacroCallNode

    data class Success(
        override val originNode: MacroCallNode,
        val tokens: List<MacroSurfaceToken>,
        val mode: MacroFragmentParser.Mode,
        /** raw builder 重新解析得到的 construction-only payload，由 stable splicer 消费。 */
        val payload: Any? = null,
    ) : MacroFragmentResult()

    data class CustomAnnotation(
        override val originNode: MacroCallNode,
        val payload: org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall,
        val tokens: List<MacroSurfaceToken>,
    ) : MacroFragmentResult()

    data class Failure(
        override val originNode: MacroCallNode,
        val reason: String,
    ) : MacroFragmentResult()
}

/**
 * Builtin non-macro surface（如 `@IfAvailable(...)`）的 desugar 入口。
 *
 * Baseline 第 8 节：builtin non-macro surface 不送 executor；fragment
 * parse 后**必须**在 stable splice 之前 desugar 为最终 CFIR。
 *
 * 本接口仅承担 desugar 决策；具体的 CFIR 节点构造由 splice 阶段实现。
 */
interface BuiltinNonMacroDesugarer {
    /**
     * 给定 [surface] 与对应 fragment 结果，返回 desugar 后的 [MacroFragmentResult]
     * （仍是 construction-only 数据）；若不支持该 surface 类型，返回 null。
     */
    fun desugar(
        surface: BuiltinNonMacroSurface,
        fragment: MacroFragmentResult.Success,
    ): MacroFragmentResult?
}

/**
 * Splice slot：surface 替换为 fragment 产物时的稳定替换槽位。
 *
 * Baseline 第 7 节"replace slots" 与第 8 节"stable splice"：splice 必须
 * 通过 handle 完成，禁止退化为 source offset fallback。
 *
 * Batch 8 阶段 slot 仅保存 handle id 与待 splice 的 fragment；
 * Batch 10 接通 owner / source / scope / symbol 修复时会在此扩展。
 */
data class MacroReplaceSlot(
    val handle: CfirReplaceHandle,
    val origin: MacroSurface,
    val fragment: MacroFragmentResult,
)

/**
 * Construction-only splice 入口：把一组 [MacroReplaceSlot] 应用到 final CFIR。
 *
 * Batch 8 阶段仅承载接口；具体实现替换由 Batch 10 的 stable-splice
 * 接入阶段完成。
 */
interface MacroStableSplicer {
    /**
     * 应用 [slots] 到 [files]，返回替换后的 file 列表。
     */
    fun applySlices(files: List<org.cangnova.cangjie.cfir.declarations.CfirFile>, slots: List<MacroReplaceSlot>): List<org.cangnova.cangjie.cfir.declarations.CfirFile>
}

/**
 * Construction-only token 重组工具：把展开后的 token 流再交给 [MacroPayloadTokenizer]
 * 风格的 evaluator 做 newTokens token-stage re-eval。
 *
 * Baseline 第 12 节 Batch 8 "newTokens token-stage re-eval"。
 * Batch 8 阶段实现只暴露接口，由调用方在 fragment parser / evaluator 中
 * 调用；具体 lexer 操作在 raw-cfir-common 内的 `MacroPayloadTokenizer`。
 */
object MacroTokenReEvaluator {
    /**
     * 对 executor 输出 token 执行一次真实 token-stage 复扫。
     *
     * 复扫器由 raw-cfir-common/PSI/LightTree 装配层注入，避免 providers
     * 反向依赖 lexer 模块；这里负责强制 construction 主流程消费复扫后的
     * token，而不是直接把原始展开文本交给语义路径。
     */
    fun reTokenize(
        tokens: List<MacroSurfaceToken>,
        tokenizer: (List<MacroSurfaceToken>) -> List<MacroSurfaceToken>,
    ): List<MacroSurfaceToken> = tokenizer(tokens)

    /**
     * 重复 [reTokenize] 直到序列稳定或达上限。
     *
     * Baseline §8 "newTokens 先 token-stage re-eval 到 stable，再 fragment parse"
     * 的纯函数式实现：调用方注入具体 lexer-backed tokenizer 后，本函数保证返回值
     * 与对其再次 lex 的结果一致（达到 fixed-point），或在 [maxIterations] 之后
     * 兜底返回最后一次结果。
     */
    fun reTokenizeUntilStable(
        tokens: List<MacroSurfaceToken>,
        tokenizer: (List<MacroSurfaceToken>) -> List<MacroSurfaceToken>,
        maxIterations: Int = 4,
    ): List<MacroSurfaceToken> {
        require(maxIterations >= 1) { "maxIterations must be >= 1, got $maxIterations" }
        var current = tokens
        repeat(maxIterations) {
            val next = tokenizer(current)
            if (next.sameSequence(current)) return next
            current = next
        }
        return current
    }

    private fun List<MacroSurfaceToken>.sameSequence(other: List<MacroSurfaceToken>): Boolean {
        if (size != other.size) return false
        for (i in indices) {
            val a = this[i]
            val b = other[i]
            if (a.text != b.text || a.kindName != b.kindName) return false
        }
        return true
    }

    /**
     * 把 [tokens] 重新拼接成字符串并按 lexer 切分。
     * 用于 child 展开后 parent args 的 stable normalization。
     */
    fun reTokenizeText(tokens: List<MacroSurfaceToken>): String = tokens.joinToString(separator = "") { it.text }

    /** 测试/未装配场景只保留文本，不允许据此宣称生产 lexer 复扫完成。 */
    fun preserveTextTokens(tokens: List<MacroSurfaceToken>): List<MacroSurfaceToken> {
        val text = reTokenizeText(tokens)
        return if (text.isEmpty()) {
            emptyList()
        } else {
            listOf(MacroSurfaceToken(text = text, startOffset = 0, endOffset = text.length))
        }
    }
}
