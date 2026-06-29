package org.cangnova.cangjie.utils

/**
 * 将列表按元素运行时类型拆分为目标类型 [B] 和剩余元素两部分。
 */
inline fun <A, reified B : A> List<A>.partitionIsInstance(): Pair<List<B>, List<A>> {
    val matching = mutableListOf<B>()
    val remaining = mutableListOf<A>()

    for (element in this) {
        if (element is B) {
            matching += element
        } else {
            remaining += element
        }
    }

    return matching to remaining
}
