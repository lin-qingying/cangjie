package org.cangnova.cangjie.utils

fun <Arg1, Bound, R> ((Arg1, Bound) -> R).bind(bound: Bound): ((Arg1) -> R) =
    { t1 -> this.invoke(t1, bound) }
