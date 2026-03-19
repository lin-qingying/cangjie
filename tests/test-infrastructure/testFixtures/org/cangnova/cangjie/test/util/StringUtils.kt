package org.cangnova.cangjie.test.util

fun Iterable<*>.joinToArrayString(): String = joinToString(separator = ", ", prefix = "[", postfix = "]")
fun Array<*>.joinToArrayString(): String = joinToString(separator = ", ", prefix = "[", postfix = "]")
