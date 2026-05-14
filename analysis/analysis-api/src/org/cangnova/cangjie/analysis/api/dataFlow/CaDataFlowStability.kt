package org.cangnova.cangjie.analysis.api.dataFlow

/**
 * 数据流稳定性等级。
 *
 * 描述表达式在求值时其引用值能否被外部修改,
 * 直接决定能否在 smart cast、常量折叠等优化中复用该值。
 *
 * 对齐 Kotlin Analysis API 的 `KaDataFlowStability` 概念,
 * 但保留仓颉自己的可变性语义边界。
 */
enum class CaDataFlowStability {
    /** 取值后不会被任何路径修改的稳定引用,可参与 smart cast。 */
    STABLE_VALUE,

    /** 可被显式赋值改变的可变引用,smart cast 不可跨语句保留。 */
    MUTABLE_VALUE,

    /** 取值产生自计算(如 getter、函数调用),每次访问可能不同。 */
    COMPUTED_VALUE,

    /** 无法判定稳定性(信息缺失或属于 error type)。 */
    UNKNOWN,
}
