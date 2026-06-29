package org.cangnova.cangjie.arguments.dsl.base

/**
 * 限定编译器参数 DSL 的接收者作用域，避免嵌套 level 或 argument builder 之间发生隐式接收者混用。
 */
@DslMarker
annotation class CangJieArgumentsDslMarker
