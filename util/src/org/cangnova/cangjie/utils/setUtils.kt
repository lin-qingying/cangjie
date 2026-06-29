package org.cangnova.cangjie.utils

/**
 * 将每个元素映射为可空集合并合并为 Set。
 *
 * 任一元素映射结果为 null 时直接返回 null；否则返回去重后的结果集合。
 */
inline fun <T, R> Iterable<T>.flatMapToNullableSet(transform: (T) -> Iterable<R>?): Set<R>? =
    flatMapTo(mutableSetOf()) { transform(it) ?: return null }.ifEmpty { emptySet() }
