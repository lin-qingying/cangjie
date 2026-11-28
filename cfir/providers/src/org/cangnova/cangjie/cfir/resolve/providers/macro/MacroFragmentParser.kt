package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.CjParsingDiagnosticPublication
import org.cangnova.cangjie.parsing.ParsingFileFacts

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
         * 解析入口、自定义注解重分类、payload 类型语义有变化时递增。
         */
        // v17：片段 raw 构建恢复实际 owner/receiver/target/参数签名，不再仅恢复包名。
        // v18：主构造器候选使用真实原始名字，直接错名成员与宏 input 分别收尾。
        // v19：宏文件及片段的 lazy 正文保留完整 parser seed。
        // v20：真实 invocation 入口、opaque token、NL 及 parser-owned diagnostics 的解析协议更新。
        // v21：直接 roots、精确 ExprKind 与 IfAvailable/参数 parser 诊断。
        // v22：生产重解析使用原生 ParseNodes 入口，保留混合 roots、参数签名与 parser 根结果。
        // v23：stable splice 按声明/参数槽接受多根结果，并在替换前校验根形状。
        // v24：片段语法失败保留 token source map，使 parser 辅助范围也映射回宿主源码。
        // v25：显式保留 token 来源、EOF 锚点，并拒绝未达到稳定点的复扫结果。
        // v26：文件模块、角色、首错误与 quote 深度统一进入根解析事务，回滚和子 parser 共享状态。
        // v27：角色检查移入真实声明文法，并按非空作用域规则保存检查顺序。
        // v28：包头、foreign 和宏显式作用域贯通，保留破损声明短路及构造器的最终属性次序。
        // v29：原生 scope 成为检查输入，包头注解与有效属性驱动的 extend/foreign 规则统一。
        // v30：宏输入构造项属性沿实际 parser 返回传播，诊断采用真实体/前缀范围。
        // v33：惰性 parser 根以真实父槽位发布冻结事件，collapse/rollback 恢复完整区域身份。
        // v40：const 的扫描终点统一识别上下文主构造器名字，保留 LPAR/LT 文法边界。
        // v46：诊断从 producer 保存独立顺序，typed/SDK 错误、正文/lambda 与宏映射共用报告事务。
        // v47：LEX 冻结报告按实际输入切片适配位置，生成输入不会被错误归入继承来源。
        // v48：参数修饰符消息在实际入队点冻结诊断顺序，倒序物化仅复用已分配的载体。
        // v49：独立根诊断时钟作为结果发布，不覆盖 parser seed 的父区域输入。
        // v50：普通函数缺名不额外消费 lookahead，保留所有入口中的后续声明与签名恢复。
        // v51：文件与原生注解的缓冲解析共享冻结输入和正式诊断发布 owner。
        // v52：当前 PSI 坐标视图与历史根输入分离，实际 LEX 叶保存 producer；原生片段提交发布真实结果。
        // v53：原生根通过实际CharTable在POM事务完成后交接；PSI来源视图原子安装并响应元数据变化。
        // v54：静态构造器先完成自身CJMP检查，再由父容器检查直接成员，保持官方producer先报。
        // v55：原生AST adoption撤销SDK旧根查表key，正式effective table与当前FileElement字段保持同代。
        const val VERSION: Int = 57
    }
}

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
    /** 构造事务中的宿主文件事实；生产 parser 必须显式继承，不能从声明状态反推。 */
    val fileFacts: ParsingFileFacts? = null,
    /** 当前片段只可写入所属宿主文件的私有事务，不能从 session 猜测活动 writer。 */
    val annotationMetadata: CfirAnnotationMetadataBuilder,
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
    /** parser 实际完成后的冻结事实；尚未执行 parser 的结果没有该值。 */
    open val parsedFileFacts: ParsingFileFacts? get() = null
    /** 成功和失败均保留最终 parser 产品；临时重扫结果不在此公共结果中累积。 */
    open val diagnosticPublication: CjParsingDiagnosticPublication get() = CjParsingDiagnosticPublication.EMPTY

    /** 语法错误保持在 parser 通道；该结果不能 splice、不能转成 CFIR 语义工厂。 */
    data class SyntaxFailure(
        override val originNode: MacroCallNode,
        val parsedText: String,
        override val diagnosticPublication: CjParsingDiagnosticPublication,
        override val parsedFileFacts: ParsingFileFacts? = null,
        /** 片段 parser 范围到原始 token 来源的映射。 */
        val sourceMap: MacroFragmentSourceMap? = null,
    ) : MacroFragmentResult() {
        val diagnostics: List<ParsingDiagnostic> get() = diagnosticPublication.diagnostics
    }

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
        val payload: MacroFragmentPayload = MacroFragmentPayload.EMPTY,
        override val parsedFileFacts: ParsingFileFacts? = null,
        /** 重新解析节点的 source 区间到原始 token 来源的映射。 */
        val sourceMap: MacroFragmentSourceMap? = null,
        override val diagnosticPublication: CjParsingDiagnosticPublication = CjParsingDiagnosticPublication.EMPTY,
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
        val payload: CfirAnnotationCall,
        /** re-evaluation 后稳定的 token 流。 */
        val tokens: List<MacroSurfaceToken>,
        override val parsedFileFacts: ParsingFileFacts? = null,
        /** 注解实参中重解析语法节点到原始注解 token 的来源映射。 */
        val sourceMap: MacroFragmentSourceMap? = null,
        override val diagnosticPublication: CjParsingDiagnosticPublication = CjParsingDiagnosticPublication.EMPTY,
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
        override val parsedFileFacts: ParsingFileFacts? = null,
    ) : MacroFragmentResult()
}

/** raw reparse 的完整结果：语法不合法与合法语法未产生所需槽位形状分别表达。 */
sealed interface MacroFragmentReparseResult {
    val parsedFileFacts: ParsingFileFacts?
    val diagnosticPublication: CjParsingDiagnosticPublication get() = CjParsingDiagnosticPublication.EMPTY
    data class Success(
        val payload: MacroFragmentPayload,
        override val parsedFileFacts: ParsingFileFacts? = null,
        override val diagnosticPublication: CjParsingDiagnosticPublication = CjParsingDiagnosticPublication.EMPTY,
    ) : MacroFragmentReparseResult

    data class AnnotationSuccess(
        val annotation: CfirAnnotationCall,
        override val parsedFileFacts: ParsingFileFacts? = null,
        override val diagnosticPublication: CjParsingDiagnosticPublication = CjParsingDiagnosticPublication.EMPTY,
    ) : MacroFragmentReparseResult
    data class Failure(val reason: String, override val parsedFileFacts: ParsingFileFacts? = null) : MacroFragmentReparseResult
    data class SyntaxFailure(
        val parsedText: String,
        override val diagnosticPublication: CjParsingDiagnosticPublication,
        override val parsedFileFacts: ParsingFileFacts? = null,
    ) : MacroFragmentReparseResult {
        val diagnostics: List<ParsingDiagnostic> get() = diagnosticPublication.diagnostics
    }
}

/**
 * Splice slot：surface 替换为 fragment 产物时的稳定替换槽位。
 *
 * Baseline 第 7 节"replace slots" 与第 8 节"stable splice"：splice 必须
 * 通过 handle 完成，禁止退化为 source offset fallback。
 *
 * Batch 8 阶段 slot 仅保存 handle id 与待 splice 的 fragment；
 * Batch 10 接通 owner / source / scope / symbol 修复时会在此扩展。
 *
 * @property handle stable splice 必须使用的 replace handle。
 * @property origin 产生该替换槽位的原始 macro surface。
 * @property fragment 将要写入槽位的 fragment 结果。
 */
data class MacroReplaceSlot(
    /** stable splice 必须使用的 replace handle。 */
    val handle: CfirReplaceHandle,
    /** 产生该替换槽位的原始 macro surface。 */
    val origin: MacroSurface,
    /** 将要写入槽位的 fragment 结果。 */
    val fragment: MacroFragmentResult,
    /** 槽位实际所属文件事务；splice 的 owner/index 变化必须在同一次操作中登记。 */
    val annotationMetadata: CfirAnnotationMetadataBuilder,
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
     * 与对其再次 lex 的结果一致（达到 fixed-point）；复扫器违反稳定输入契约时直接失败。
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
        error("Macro token re-evaluation did not reach a stable sequence after $maxIterations iterations.")
    }

    /** 文本、种类和来源坐标共同构成稳定输入；来源变化也必须经过下一轮复扫确认。 */
    private fun List<MacroSurfaceToken>.sameSequence(other: List<MacroSurfaceToken>): Boolean {
        if (size != other.size) return false
        for (i in indices) {
            val a = this[i]
            val b = other[i]
            if (a != b) return false
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
