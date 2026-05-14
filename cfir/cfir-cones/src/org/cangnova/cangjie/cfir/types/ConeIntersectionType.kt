package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.IntersectionTypeConstructorMarker

/**
 * 交叉类型，对应仓颉编译器中的 IntersectionTy。
 * 内部类型检查使用，表示多个类型的交叉。
 */
class ConeIntersectionType(
    val intersectedTypes: Collection<ConeCangJieType>,
    /**
     * 当约束系统已经知道交叉结果的稳定上界时，按 Kotlin FIR 主干把它挂在交叉类型本身，
     * 供近似阶段消费，而不是在近似时重新拼接一个“交叉 + 上界”的伪结果。
     */
    val upperBoundForApproximation: ConeCangJieType? = null,
    override val attributes: ConeAttributes = intersectedTypes.intersectedAttributes(),
) : ConeSimpleCangJieType(), IntersectionTypeConstructorMarker, ConeTypeConstructorMarker {
    override fun equals(other: Any?): Boolean =
        other is ConeIntersectionType && intersectedTypes == other.intersectedTypes

    override fun hashCode(): Int = intersectedTypes.hashCode()


}

/**
 * 联合类型，对应仓颉编译器中的 UnionTy。
 * 内部类型检查使用，表示多个类型的联合。
 */
class ConeUnionType(
    val unionTypes: Set<ConeCangJieType>,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {
    override fun equals(other: Any?): Boolean =
        other is ConeUnionType && unionTypes == other.unionTypes

    override fun hashCode(): Int = unionTypes.hashCode()


}

private fun Collection<ConeCangJieType>.intersectedAttributes(): ConeAttributes {
    val iterator = iterator()
    if (!iterator.hasNext()) return ConeAttributes.Empty

    var result = iterator.next().attributes
    while (iterator.hasNext()) {
        result = result.intersect(iterator.next().attributes)
    }
    return result
}
