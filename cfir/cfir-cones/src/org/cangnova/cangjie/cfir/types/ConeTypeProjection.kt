package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.TypeArgumentMarker

/**
 * Cone 类型实参投影的根类。
 *
 * 仓颉当前没有 Kotlin 风格的 `in` / `out` / star projection，
 * 因此投影根主要用于和公共类型系统 marker 对接，并允许后续扩展。
 */
sealed class ConeTypeProjection : TypeArgumentMarker {

    /**
     * 投影工具常量。
     */
    companion object {
        /**
         * 无实参场景复用的空数组。
         */
        val EMPTY_ARRAY: Array<ConeTypeProjection> = arrayOf()
    }
}

/**
 * 直接承载 [ConeCangJieType] 的类型投影视图。
 */
sealed class ConeCangJieTypeProjection : ConeTypeProjection() {
    /**
     * 当前投影对应的具体仓颉类型。
     */
    abstract val type: ConeCangJieType

    /**
     * 类型投影按内部类型做结构相等。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeCangJieTypeProjection) return false

        if (type != other.type) return false

        return true
    }

    /**
     * 类型投影的结构哈希。
     */
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
