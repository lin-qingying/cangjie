package org.cangnova.cangjie.analysis.api.symbols

/**
 * 符号的模态（modality）。
 *
 * 模态描述声明是否可被继承/重写，是类型层与成员层共同关心的语义维度。
 *
 * 对齐 Kotlin Analysis API 的 `KaSymbolModality`。
 */
enum class CaSymbolModality {
    /**
     * 终态声明：不可被继承或重写。仓颉的默认模态。
     */
    FINAL,

    /**
     * 密封（sealed）声明：只允许在受控范围内继承。
     *
     * 所有直接子类型在编译期已知，便于穷尽匹配等语义校验。
     */
    SEALED,

    /**
     * 开放（open）声明：带有默认实现，且允许被子类型继承或重写。
     */
    OPEN,

    /**
     * 抽象（abstract）声明：不带实现，必须由具体实现填补。
     */
    ABSTRACT,
}
