package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration

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

    /** Fragment parser 的解析入口模式。 */
    enum class Mode {
        /** 将 token 流解析为表达式 fragment。 */
        EXPRESSION,
        /** 将 token 流解析为声明 fragment。 */
        DECLARATION,
        /** 将 annotation slot snapshot 重新解析为 custom annotation。 */
        CUSTOM_ANNOTATION,
    }

    companion object {
        /**
         * Fragment parser 算法版本。PLAN.md §11 cache key 第 12 维。
         *
         * 解析入口、custom-annotation fallback、payload 类型语义有变化时递增。
         */
        // v4: declaration fragments carry the complete zero-to-many declaration sequence.
        const val VERSION: Int = 4
    }
}

/**
 * 声明宏重解析阶段的完整输出序列。
 *
 * 该对象只存在于 construction/splice 边界：声明宏可以合法地产生零个、一个或多个
 * 声明，不能把 fragment 收窄成单个声明或额外塞入一个语义 CFIR 包装节点。
 */
data class MacroDeclarationFragment(
    /** 按宏输出源码顺序排列的全部重解析声明。 */
    val declarations: List<CfirDeclaration>,
)

/**
 * fragment parser 的完整输入。
 *
 * @property node 当前正在解析的 macro forest 节点。
 * @property tokens executor 或 builtin evaluator 产出的 token 流。
 * @property decision 当前 surface 的最终 routing 决策。
 * @property annotationSnapshot custom annotation 模式所需的原始 annotation 槽位 snapshot。
 */
data class MacroFragmentInput(
    /** 当前正在解析的 macro forest 节点。 */
    val node: MacroCallNode,
    /** executor 或 builtin evaluator 产出的 token 流。 */
    val tokens: List<MacroSurfaceToken>,
    /** 当前 surface 的最终 routing 决策。 */
    val decision: FinalMacroSurfaceDecision,
    /** custom annotation 模式所需的原始 annotation 槽位 snapshot。 */
    val annotationSnapshot: CfirAnnotationSlotSnapshot? = null,
) {
    /** 从最终 routing 决策投影出的解析模式。 */
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
    /** 产生该 fragment 结果的原始 macro forest 节点。 */
    abstract val originNode: MacroCallNode

    /**
     * fragment 解析成功。
     *
     * @property originNode 产生该结果的原始 macro forest 节点。
     * @property tokens re-evaluation 后稳定的 token 流。
     * @property mode 本次 fragment 解析模式。
     * @property payload raw builder 返回的 construction-only payload。
     */
    data class Success(
        /** 产生该结果的原始 macro forest 节点。 */
        override val originNode: MacroCallNode,
        /** re-evaluation 后稳定的 token 流。 */
        val tokens: List<MacroSurfaceToken>,
        /** 本次 fragment 解析模式。 */
        val mode: MacroFragmentParser.Mode,
        /** raw builder 重新解析得到的 construction-only payload，由 stable splicer 消费。 */
        val payload: Any? = null,
    ) : MacroFragmentResult()

    /**
     * custom annotation fallback 解析成功。
     *
     * @property originNode 产生该结果的原始 macro forest 节点。
     * @property payload 重新解析得到的新 annotation 调用。
     * @property tokens re-evaluation 后稳定的 token 流。
     */
    data class CustomAnnotation(
        /** 产生该结果的原始 macro forest 节点。 */
        override val originNode: MacroCallNode,
        /** 重新解析得到的新 annotation 调用。 */
        val payload: org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall,
        /** re-evaluation 后稳定的 token 流。 */
        val tokens: List<MacroSurfaceToken>,
    ) : MacroFragmentResult()

    /**
     * fragment 解析失败。
     *
     * @property originNode 产生该结果的原始 macro forest 节点。
     * @property reason 失败原因，供 construction diagnostic 转换使用。
     */
    data class Failure(
        /** 产生该结果的原始 macro forest 节点。 */
        override val originNode: MacroCallNode,
        /** 失败原因，供 construction diagnostic 转换使用。 */
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
 * @property handle stable splice 必须使用的 replace handle。
 * @property origin 产生该替换槽位的原始 macro surface。
 * @property fragment 将要写入槽位的 fragment 结果。
 * @property embeddedSurfaceIds 其展开 token 已被本 fragment 重新解析并包含在其中的后代 surface。
 */
data class MacroReplaceSlot(
    /** stable splice 必须使用的 replace handle。 */
    val handle: CfirReplaceHandle,
    /** 产生该替换槽位的原始 macro surface。 */
    val origin: MacroSurface,
    /** 将要写入槽位的 fragment 结果。 */
    val fragment: MacroFragmentResult,
    /** 后代 surface 的展开结果已进入本 fragment，因而不再对应原始 final-CFIR 载体。 */
    val embeddedSurfaceIds: Set<Long> = emptySet(),
)

/**
 * 记录 stable splice 实际应用的一条声明替换关系。
 *
 * Cangjie 声明宏可以把一个 carrier 替换成零个、一个或多个声明；每个 replacement
 * 都保留与原始 carrier 的对象身份关系，供 Analysis/IDE 构建源码结构和语义快照使用。
 */
data class MacroAppliedDeclarationReplacement(
    /** 被宏替换的原始声明 carrier。 */
    val carrier: org.cangnova.cangjie.cfir.declarations.CfirDeclaration,
    /** 本次 splice 在 carrier 槽位中写入的全部声明；空列表表示该宏删除了 carrier。 */
    val replacements: List<org.cangnova.cangjie.cfir.declarations.CfirDeclaration>,
    /** 产生此次替换的原始宏 surface。 */
    val surfaceId: Long,
)

/**
 * 记录表达式宏 stable splice 实际写入的精确表达式身份关系。
 *
 * carrier 是 raw CFIR 中的 construction-only 表达式，replacement 是同一最终文件树中的语义表达式。
 * Analysis/IDE 通过这条关系把物理宏调用 PSI 映射到展开后的表达式，而不根据文本或偏移猜测节点。
 */
data class MacroAppliedExpressionReplacement(
    /** 被宏替换的 raw 表达式 carrier。 */
    val carrier: org.cangnova.cangjie.cfir.expressions.CfirExpression,
    /** 本次 splice 在 carrier 槽位中写入的最终表达式。 */
    val replacement: org.cangnova.cangjie.cfir.expressions.CfirExpression,
    /** 产生此次替换的原始宏 surface。 */
    val surfaceId: Long,
)

/** stable splice 的最终 CFIR 与精确声明来源关系。 */
data class MacroStableSpliceResult(
    /** 执行完所有 stable replacements 的文件。 */
    val files: List<org.cangnova.cangjie.cfir.declarations.CfirFile>,
    /** 仅包含实际应用的声明替换，不包含 embedded surface 或 annotation-only 替换。 */
    val declarationReplacements: List<MacroAppliedDeclarationReplacement>,
    /** 仅包含实际应用的表达式替换，不包含由父 surface 重新解析吸收的 embedded surface。 */
    val expressionReplacements: List<MacroAppliedExpressionReplacement> = emptyList(),
)

/**
 * Construction-only splice 入口：把一组 [MacroReplaceSlot] 应用到 final CFIR。
 *
 * Batch 8 阶段仅承载接口；具体实现替换由 Batch 10 的 stable-splice
 * 接入阶段完成。
 */
interface MacroStableSplicer {
    /**
     * 应用 [slots] 到 [files]，并同时返回替换后的文件与实际声明替换来源。
     *
     * 来源关系必须在按 replace-handle carrier 身份完成替换时产生；调用方不得从名称、文本或偏移反推。
     */
    fun applySlices(
        files: List<org.cangnova.cangjie.cfir.declarations.CfirFile>,
        slots: List<MacroReplaceSlot>,
    ): MacroStableSpliceResult
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

    /** 比较两个 token 序列的文本与 token kind 是否完全一致。 */
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
