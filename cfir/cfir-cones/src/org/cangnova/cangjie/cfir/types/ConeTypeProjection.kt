package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.TypeArgumentMarker

/**
 * 仓颉泛型实参。
 *
 * 仓颉没有 Kotlin 的 `in` / `out` 投影和星号投影，
 * 因而一个类型实参就是一个确定的具体类型。
 */
class ConeTypeProjection(
    val type: ConeCangJieType,
) : TypeArgumentMarker {
    override fun equals(other: Any?): Boolean =
        other is ConeTypeProjection && type == other.type

    override fun hashCode(): Int = type.hashCode()

    override fun toString(): String = type.toString()
}
