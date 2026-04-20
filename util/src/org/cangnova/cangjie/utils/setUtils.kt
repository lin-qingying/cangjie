package org.cangnova.cangjie.utils

inline fun <T, R> Iterable<T>.flatMapToNullableSet(transform: (T) -> Iterable<R>?): Set<R>? =
    flatMapTo(mutableSetOf()) { transform(it) ?: return null }.ifEmpty { emptySet() }
