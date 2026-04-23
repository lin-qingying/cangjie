package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.TypeArgumentMarker
sealed class ConeTypeProjection : TypeArgumentMarker {

    companion object {
        val EMPTY_ARRAY: Array<ConeTypeProjection> = arrayOf()
    }
}
sealed class ConeCangJieTypeProjection : ConeTypeProjection() {
    abstract val type: ConeCangJieType

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeCangJieTypeProjection) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode() * 31
    }
}

/**
 * 将投影还原为具体仓颉类型。
 *
 * 当前仓颉没有星投影与变型投影，因此合法的 [ConeTypeProjection]
 * 必然对应一个确定的 [ConeCangJieType]。
 */
val ConeTypeProjection.type: ConeCangJieType
    get() = when (this) {
        is ConeCangJieTypeProjection -> type
    }

/**
 * 仓颉泛型实参。
 *
 * 仓颉没有 CangJie 的 `in` / `out` 投影和星号投影，
 * 因而一个类型实参就是一个确定的具体类型。
 */
//class ConeTypeProjection(
//    val type: ConeCangJieType,
//) : TypeArgumentMarker {
//    override fun equals(other: Any?): Boolean =
//        other is ConeTypeProjection && type == other.type
//
//    override fun hashCode(): Int = type.hashCode()
//
//    override fun toString(): String = type.toString()
//}
