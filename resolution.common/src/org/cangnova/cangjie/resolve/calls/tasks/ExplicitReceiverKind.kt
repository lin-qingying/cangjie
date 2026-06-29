/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0
 */
package org.cangnova.cangjie.resolve.calls.tasks

/**
 * 函数调用中显式接收者的类型
 * 
 * 描述一个候选函数在调用时，显式接收者的来源和种类。
 * 
 * ## 背景概念
 * 
 * 仓颉中函数调用存在两种接收者：
 * 
 * - **扩展接收者（Extension Receiver）**：扩展函数的接收者。
 * 例如 `"hello".length` 中的 `"hello"` 是 String 扩展属性的扩展接收者。
 * 
 * - **派发接收者（Dispatch Receiver）**：成员函数所属对象的接收者。
 * 例如 `obj.foo()` 中的 `obj` 是成员函数 `foo` 的派发接收者。
 * 
 * ## 特殊情况：双接收者
 * 
 * 当一个类的扩展成员函数被调用时，可能同时存在两个显式接收者。
 * 
 * 例如：
 * ```
 * class Foo {
 * fun Bar.invoke(x: Int64) { }
 * }
 * 
 * // 调用：foo.bar(1)
 * // bar 作为扩展接收者，foo 作为派发接收者
 * ```
 * 
 * 此时适用性为 [BOTH_RECEIVERS]。
 */
enum class ExplicitReceiverKind {
    /**
     * 显式接收者是扩展接收者。
     *
     * 例如：`obj.extFun()` 中，`obj` 是 extend 成员的扩展接收者。
     */
    EXTENSION_RECEIVER,

    /**
     * 显式接收者是派发接收者。
     * 
     * 例如：`obj.foo()` 中，`obj` 是成员函数 `foo` 的派发接收者。
     */
    DISPATCH_RECEIVER,

    /**
     * 没有显式接收者。
     * 
     * 例如：直接调用顶层函数 `foo()`，或在当前作用域内调用成员函数。
     */
    NO_EXPLICIT_RECEIVER,

    /**
     * 同时存在扩展接收者和派发接收者。
     *
     * 对齐 Kotlin 的特殊调用形态：成员扩展调用可能把一个显式 receiver
     * 作为 extension receiver，同时已有 dispatch receiver。
     */
    BOTH_RECEIVERS,
    ;

    /**
     * 是否存在扩展接收者。
     */
    val isExtensionReceiver: Boolean
        /**
         * 判断此调用是否有扩展接收者。
         *
         * [EXTENSION_RECEIVER] 和 [BOTH_RECEIVERS] 均返回 true。
        */
        get() = this == ExplicitReceiverKind.EXTENSION_RECEIVER || this == ExplicitReceiverKind.BOTH_RECEIVERS

    /**
     * 是否存在派发接收者。
     */
    val isDispatchReceiver: Boolean
        /**
         * 判断此调用是否有派发接收者。
         * 
         * [DISPATCH_RECEIVER] 和 [BOTH_RECEIVERS] 均返回 true。
         * 
         * @return true 表示存在派发接收者
         */
        get() = this == ExplicitReceiverKind.DISPATCH_RECEIVER || this == ExplicitReceiverKind.BOTH_RECEIVERS
}
