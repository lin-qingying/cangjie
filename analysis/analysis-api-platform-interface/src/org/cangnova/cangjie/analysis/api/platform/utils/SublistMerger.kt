package org.cangnova.cangjie.analysis.api.platform.utils

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.partitionIsInstance

/**
 * 对列表中特定子类型分段合并的工具。
 */
@CaPlatformInterface
class SublistMerger<A : Any>(
    initialElements: List<A>,
    /**
     * 合并结果写入的目标列表。
     */
    val destination: MutableList<A>,
) {
    /**
     * 尚未被合并规则消费的元素。
     */
    var remainingElements: List<A> = initialElements

    /**
     * 取出剩余元素中的 [R] 类型子列表，调用 [create] 生成合并结果。
     */
    inline fun <reified R : A> merge(create: (List<R>) -> A?) {
        val (specificElements, remainingElements) = this.remainingElements.partitionIsInstance<_, R>()
        destination.addIfNotNull(create(specificElements))
        this.remainingElements = remainingElements
    }

    /**
     * 将剩余未消费元素追加到目标列表并清空剩余列表。
     */
    fun finish() {
        destination.addAll(remainingElements)
        remainingElements = emptyList()
    }
}

/**
 * 将当前列表按 [SublistMerger] 规则合并到 [destination]。
 */
@CaPlatformInterface
fun <A : Any> List<A>.mergeInto(destination: MutableList<A>, f: SublistMerger<A>.() -> Unit) {
    SublistMerger(this, destination).apply {
        f()
        finish()
    }
}

/**
 * 返回当前列表按 [SublistMerger] 规则合并后的新列表。
 */
@CaPlatformInterface
fun <A : Any> List<A>.mergeWith(f: SublistMerger<A>.() -> Unit): List<A> =
    mutableListOf<A>().also { destination -> mergeInto(destination, f) }

/**
 * 仅合并当前列表中指定类型 [R] 的元素。
 */
@CaPlatformInterface
inline fun <A : Any, reified R : A> List<A>.mergeOnly(crossinline create: (List<R>) -> A?): List<A> =
    mergeWith { merge<R>(create) }
