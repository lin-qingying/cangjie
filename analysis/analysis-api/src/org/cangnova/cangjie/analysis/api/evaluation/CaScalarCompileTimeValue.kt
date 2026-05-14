package org.cangnova.cangjie.analysis.api.evaluation

/**
 * 标量编译期常量。
 *
 * 表示 Bool、整数、浮点、Rune、String、Unit 等单值常量;
 * 具体种类由 [kind] 携带,调用方据此分支处理或转换为目标字面量。
 */
interface CaScalarCompileTimeValue : CaCompileTimeValue {
    /** 当前标量值的具体种类。 */
    val kind: CaScalarValueKind
}
