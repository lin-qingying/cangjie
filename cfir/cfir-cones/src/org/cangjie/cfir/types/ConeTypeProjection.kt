package org.cangjie.cfir.types

/**
 * 类型实参（Type Argument）。
 *
 * 仓颉的泛型系统是不变的（invariant），没有 Kotlin 的 in/out 变型声明，
 * 也没有星号投影。所有泛型实参都必须是具体类型。
 */
class ConeTypeProjection(
    val type: ConeCangjieType,
) {
    override fun equals(other: Any?): Boolean =
        other is ConeTypeProjection && type == other.type

    override fun hashCode(): Int = type.hashCode()

    override fun toString(): String = type.toString()
}
