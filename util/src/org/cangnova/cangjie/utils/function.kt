package org.cangnova.cangjie.utils

/**
 * 将二元函数的第二个参数绑定为固定值，得到只接收第一个参数的一元函数。
 */
fun <Arg1, Bound, R> ((Arg1, Bound) -> R).bind(bound: Bound): ((Arg1) -> R) =
    { t1 -> this.invoke(t1, bound) }
