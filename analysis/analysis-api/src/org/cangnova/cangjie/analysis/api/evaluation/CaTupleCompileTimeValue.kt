package org.cangnova.cangjie.analysis.api.evaluation

/**
 * Tuple 编译期常量。
 *
 * 与 [CaCollectionCompileTimeValue] 不同,tuple 各位置的元素类型可不同,
 * [elements] 按 tuple 位置顺序排列。
 */
interface CaTupleCompileTimeValue : CaCompileTimeValue {
    /** Tuple 中按位置排列的常量元素。 */
    val elements: List<CaCompileTimeValue>
}
