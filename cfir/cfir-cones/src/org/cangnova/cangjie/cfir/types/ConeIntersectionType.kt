package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.IntersectionTypeConstructorMarker

/**
 * 交叉类型，对应仓颉编译器中的 IntersectionTy。
 * 内部类型检查使用，表示多个类型的交叉。
 *
 * @property intersectedTypes 参与交叉的类型集合。
 * @property upperBoundForApproximation 近似阶段可直接消费的稳定上界。
 * @property attributes 交叉类型附带的属性。
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
    /**
     * 交叉类型按交叉成员集合判等。
     */
    override fun equals(other: Any?): Boolean =
        other is ConeIntersectionType && intersectedTypes == other.intersectedTypes

    /**
     * 交叉类型的结构哈希。
     */
    override fun hashCode(): Int = intersectedTypes.hashCode()


}

/**
 * 联合类型，对应仓颉编译器中的 UnionTy。
 * 内部类型检查使用，表示多个类型的联合。
 *
 * @property unionTypes 参与联合的类型集合。
 * @property attributes 联合类型附带的属性。
 */
class ConeUnionType(
    val unionTypes: Set<ConeCangJieType>,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {
    /**
     * 联合类型按联合成员集合判等。
     */
    override fun equals(other: Any?): Boolean =
        other is ConeUnionType && unionTypes == other.unionTypes

    /**
     * 联合类型的结构哈希。
     */
    override fun hashCode(): Int = unionTypes.hashCode()


}

/**
 * 计算交叉类型成员属性的逐项交集。
 */
private fun Collection<ConeCangJieType>.intersectedAttributes(): ConeAttributes {
    val iterator = iterator()
    if (!iterator.hasNext()) return ConeAttributes.Empty

    var result = iterator.next().attributes
    while (iterator.hasNext()) {
        result = result.intersect(iterator.next().attributes)
    }
    return result
}
