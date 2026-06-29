package org.cangnova.cangjie.test.util

/**
 * 执行 `Iterable` 对应的测试工具流程，维持测试框架的阶段契约。
 */
fun Iterable<*>.joinToArrayString(): String = joinToString(separator = ", ", prefix = "[", postfix = "]")
/**
 * 执行 `Array` 对应的测试工具流程，维持测试框架的阶段契约。
 */
fun Array<*>.joinToArrayString(): String = joinToString(separator = ", ", prefix = "[", postfix = "]")
