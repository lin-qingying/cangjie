package org.cangjie.cfir.types

/**
 * 交叉类型，对应仓颉编译器中的 IntersectionTy。
 * 内部类型检查使用，表示多个类型的交叉。
 */
class ConeIntersectionType(
    val intersectedTypes: List<ConeCangjieType>,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {

    override fun equals(other: Any?): Boolean =
        other is ConeIntersectionType && intersectedTypes == other.intersectedTypes

    override fun hashCode(): Int = intersectedTypes.hashCode()

    override fun toString(): String =
        intersectedTypes.joinToString(" & ")
}

/**
 * 联合类型，对应仓颉编译器中的 UnionTy。
 * 内部类型检查使用，表示多个类型的联合。
 */
class ConeUnionType(
    val unionTypes: Set<ConeCangjieType>,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {

    override fun equals(other: Any?): Boolean =
        other is ConeUnionType && unionTypes == other.unionTypes

    override fun hashCode(): Int = unionTypes.hashCode()

    override fun toString(): String =
        unionTypes.joinToString(" | ")
}
