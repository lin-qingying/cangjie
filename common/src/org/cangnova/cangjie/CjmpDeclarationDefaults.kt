package org.cangnova.cangjie

/**
 * 对位官方 `HasDefault` 的共享判据：specific 声明先获豁免，再读取声明自身的实现事实。
 *
 * parser 和 CFIR 分别投影原生函数体、属性访问器及变量初始化器；不以所在容器或
 * 后代节点推断实现，也不在 specific 分支提前读取尚未解析的实现。
 */
public inline fun hasCjmpDefaultImplementation(
    isSpecific: Boolean,
    implementationExists: () -> Boolean,
): Boolean = isSpecific || implementationExists()
