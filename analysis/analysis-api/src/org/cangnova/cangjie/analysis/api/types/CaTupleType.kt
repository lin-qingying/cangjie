package org.cangnova.cangjie.analysis.api.types

/**
 * 仓颉元组类型(tuple type)。
 *
 * 表示形如 `(T1, T2, T3)` 的固定长度异构类型组合,元素顺序与位置语义相关。
 * 元组在仓颉中是语言级别的内建结构类型,因此公开层直接以独立 type 形式建模,
 * 不沿用 class-like type 的限定/符号视角。
 *
 * 在 Kotlin Analysis API 中并无直接对应类型,这是 Cangjie 相对 Kotlin 的偏差点之一。
 */
interface CaTupleType : CaType {
    /**
     * 元组的各位置元素类型,按声明顺序排列。
     */
    val elementTypes: List<CaType>
}
