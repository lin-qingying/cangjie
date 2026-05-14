package org.cangnova.cangjie.analysis.api.symbols

/**
 * 字段（成员变量）符号。
 *
 * 表示 class / struct / extend 体内声明的成员变量，区别于：
 * - [CaPropertySymbol]：仓颉显式 property 语法，有 getter/setter；
 * - [CaLocalVariableSymbol]：函数体内部的局部变量。
 */
abstract class CaFieldSymbol : CaVariableSymbol() {
    /**
     * 是否为静态字段。
     */
    abstract val isStatic: Boolean

    /**
     * 是否为编译期常量字段。
     */
    abstract val isConst: Boolean
}
