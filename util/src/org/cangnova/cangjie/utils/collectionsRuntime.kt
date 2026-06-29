package org.cangnova.cangjie.utils

/**
 * 当 [t] 非空时向序列构建器产出该元素。
 */
suspend fun <T : Any> SequenceScope<T>.yieldIfNotNull(t: T?) = if (t != null) yield(t) else Unit
