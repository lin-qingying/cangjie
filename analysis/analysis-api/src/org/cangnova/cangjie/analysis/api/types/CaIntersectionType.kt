package org.cangnova.cangjie.analysis.api.types

/**
 * 交集类型(intersection type)。
 *
 * 表示一个值同时属于多个类型的复合类型,形如 `A & B`。
 * 仓颉源码通常不直接书写交集类型,它一般产生于编译器内部的运算,例如:
 * - 智能转换/类型收窄;
 * - 泛型约束的合成(同时满足多个约束的 upper bound)。
 *
 * [conjuncts] 列出参与交集的各个类型,顺序与具体生成路径相关,语义上视为无序集合。
 *
 * 对齐 Kotlin Analysis API 的 `KaIntersectionType`。
 */
interface CaIntersectionType : CaType {
    /**
     * 参与交集的各个类型,语义上视为 “同时满足” 的合取。
     */
    val conjuncts: List<CaType>
}
