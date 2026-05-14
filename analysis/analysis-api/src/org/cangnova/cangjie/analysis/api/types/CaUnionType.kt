package org.cangnova.cangjie.analysis.api.types

/**
 * 联合类型(union type)。
 *
 * 表示一个值可以属于若干候选类型之一的复合类型,形如 `A | B`。
 * 类似 [CaIntersectionType],联合类型通常不直接出现在源码中,而是由编译器内部计算产生,
 * 例如多分支 if/match 表达式的合并类型。
 *
 * [alternatives] 列出可选项的各个类型,语义上视为无序集合(析取)。
 *
 * Kotlin 当前没有公开的联合类型(`KaUnionType`),这是 Cangjie 相对 Kotlin Analysis API 的扩展点。
 */
interface CaUnionType : CaType {
    /**
     * 参与联合的各个候选类型,语义上视为 “或” 的析取。
     */
    val alternatives: List<CaType>
}
