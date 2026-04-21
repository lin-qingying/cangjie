package org.cangnova.cangjie.cfir.expressions

/**
 * `match` 穷尽性正式承载层。
 *
 * 设计目标：
 * - 作为 CFIR tree 层的稳定 carrier，避免 side table；
 * - 同时承载 `Exhaustive / NonExhaustive / Unknown / Error` 真实状态；
 * - 不依赖 checkers 模块类型，保证 resolve 与 checker 可共享读写。
 */
sealed class CfirMatchExhaustivenessStatus {
    /**
     * 当前状态来自哪个阶段。
     *
     * `Unknown` 仅表示“来源未知”，不等价于穷尽性结论。
     */
    enum class Source {
        Unknown,
        BodyResolve,
        Checker,
    }

    abstract val source: Source

    /**
     * 尚未计算，或当前阶段无法给出可信结论。
     */
    data object Unknown : CfirMatchExhaustivenessStatus() {
        override val source: Source = Source.Unknown
    }

    /**
     * 已证明穷尽。
     */
    data class Exhaustive(
        override val source: Source = Source.Unknown,
    ) : CfirMatchExhaustivenessStatus()

    /**
     * 已证明不穷尽，缺失分支以文本形式承载，便于诊断复用。
     */
    data class NonExhaustive(
        val missingCaseTexts: List<String>,
        override val source: Source = Source.Unknown,
    ) : CfirMatchExhaustivenessStatus()

    /**
     * 计算流程出错（例如矩阵构建失败）时的状态承载。
     */
    data class Error(
        val reason: String,
        override val source: Source = Source.Unknown,
    ) : CfirMatchExhaustivenessStatus()
}
