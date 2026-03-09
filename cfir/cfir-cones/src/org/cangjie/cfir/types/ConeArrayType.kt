package org.cangjie.cfir.types

/**
 * 数组类型（RawArray），对应仓颉编译器中的 ArrayTy。
 *
 * RawArray 是仓颉中的低级数组类型。
 */
class ConeArrayType(
    val elementType: ConeCangjieType,
    /** 数组维度 */
    val dims: Int = 1,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {

    override val typeArguments: List<ConeCangjieType> get() = listOf(elementType)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeArrayType) return false
        return elementType == other.elementType && dims == other.dims
    }

    override fun hashCode(): Int {
        var result = elementType.hashCode()
        result = 31 * result + dims
        return result
    }

    override fun toString(): String = "Array<$elementType>"
}

/**
 * 定长数组类型（VArray），对应仓颉编译器中的 VArrayTy。
 *
 * VArray<T, $N> 是编译时确定大小的数组。
 */
class ConeVArrayType(
    val elementType: ConeCangjieType,
    /** 数组大小（编译时常量） */
    val size: Long,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {

    override val typeArguments: List<ConeCangjieType> get() = listOf(elementType)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeVArrayType) return false
        return elementType == other.elementType && size == other.size
    }

    override fun hashCode(): Int {
        var result = elementType.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }

    override fun toString(): String = "VArray<$elementType, $size>"
}
