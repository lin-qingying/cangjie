package org.cangnova.cangjie.analysis.api.components

/**
 * 展开面的可靠性三态。
 *
 * 三态而非布尔：「位置在宏调用内但映射不可靠」与「位置根本不在宏调用内」对消费方是两件事——
 * 前者只能保守处理，后者与引入映射之前完全一致。
 */
enum class CaMacroExpansionReliability {
    /** 展开面可靠：其载体通道已解析。 */
    RELIABLE,

    /** 展开面存在但载体通道处于未解析状态，或同一偏移命中多个互不嵌套的展开面。 */
    UNRELIABLE,

    /** 位置不在任何宏调用区间内。 */
    NONE,
}

/**
 * 「位置 → 宏展开源映射」的只读结论。
 *
 * 只承载**身份 + 区间 + 可靠性**三样纯数据，不携带展开产物：
 * `MacroSurface*` 不实现 `CfirElement`、不进入 provider-visible final CFIR 的边界
 * 不因本查询而改变，调用方也拿不到 surface 本身。
 */
sealed interface CaMacroExpansionMapping {
    /** 可靠性三态，与具体形态一致。 */
    val reliability: CaMacroExpansionReliability

    /** 位置不在任何宏调用区间内。 */
    data object None : CaMacroExpansionMapping {
        override val reliability: CaMacroExpansionReliability
            get() = CaMacroExpansionReliability.NONE
    }

    /**
     * 同一偏移命中多个**互不嵌套**的展开面，无法唯一确定最内层。
     *
     * 单独成态而不是退化成「无展开」：消费方对它必须保守（等同不可靠），
     * 而「无展开」是「行为与引入映射之前一致」。
     */
    data object Ambiguous : CaMacroExpansionMapping {
        override val reliability: CaMacroExpansionReliability
            get() = CaMacroExpansionReliability.UNRELIABLE
    }

    /**
     * 位置落在唯一的最内层展开面区间内。
     *
     * @property surfaceId 展开面的会话内稳定身份（不跨会话稳定）。
     * @property startOffset 展开面在宿主源码中的起始偏移（含）。
     * @property endOffset 展开面在宿主源码中的结束偏移（含）。
     * @property reliability 只能是 [CaMacroExpansionReliability.RELIABLE] 或
     *   [CaMacroExpansionReliability.UNRELIABLE]：能走到这里就说明位置落在某个展开面内。
     */
    data class Inside(
        val surfaceId: Long,
        val startOffset: Int,
        val endOffset: Int,
        override val reliability: CaMacroExpansionReliability,
    ) : CaMacroExpansionMapping
}
