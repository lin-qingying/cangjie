package org.cangnova.cangjie.analysis.api.evaluation

/**
 * 集合(Array、List 等同种元素容器)的编译期常量。
 *
 * [elements] 按出现顺序保留容器内每个元素的常量视图,
 * 元素本身仍是 [CaCompileTimeValue],可递归 narrow。
 */
interface CaCollectionCompileTimeValue : CaCompileTimeValue {
    /** 容器内所有常量元素,按定义顺序排列。 */
    val elements: List<CaCompileTimeValue>
}
