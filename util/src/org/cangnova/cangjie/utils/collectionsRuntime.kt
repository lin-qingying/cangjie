package org.cangnova.cangjie.utils

suspend fun <T : Any> SequenceScope<T>.yieldIfNotNull(t: T?) = if (t != null) yield(t) else Unit
