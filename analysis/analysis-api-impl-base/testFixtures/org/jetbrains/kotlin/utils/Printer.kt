package org.jetbrains.kotlin.utils

/**
 * Kotlin 测试生成器命名空间下的打印器别名。
 *
 * 该别名让复用自 Kotlin 的生成器代码继续引用 `org.jetbrains.kotlin.utils.Printer`，
 * 实际实现则委派到仓颉项目自己的 [org.cangnova.cangjie.utils.Printer]。
 */
typealias Printer = org.cangnova.cangjie.utils.Printer
