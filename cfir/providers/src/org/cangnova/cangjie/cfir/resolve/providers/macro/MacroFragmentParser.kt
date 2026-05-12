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
     * 解析 [tokens]（来自 macro evaluator 输出）为 [MacroFragmentResult]。
     *
     * @param node 触发本次解析的 forest 节点（提供 owner / scope / source 上下文）
     * @param tokens 待解析的 token 流
     * @param mode 解析模式，决定 grammar 入口：
     *             - [Mode.EXPRESSION] : 期望表达式产物
     *             - [Mode.DECLARATION] : 期望声明产物
     *             - [Mode.CUSTOM_ANNOTATION] : custom annotation 回退路径
     */
    fun parse(
        node: MacroCallNode,
        tokens: List<MacroSurfaceToken>,
        mode: Mode,
    ): MacroFragmentResult

    enum class Mode { EXPRESSION, DECLARATION, CUSTOM_ANNOTATION }
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
        val annotationName: org.cangnova.cangjie.name.Name,
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
     * 把 [tokens] 重新拼接成字符串并按 lexer 切分。
     * 用于 child 展开后 parent args 的 stable normalization。
     */
    fun reTokenizeText(tokens: List<MacroSurfaceToken>): String = tokens.joinToString(separator = "") { it.text }
}
