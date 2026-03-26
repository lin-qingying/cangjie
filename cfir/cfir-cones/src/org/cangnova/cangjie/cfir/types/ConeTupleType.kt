package org.cangnova.cangjie.cfir.types

/**
 * 元组类型。
 * 仓颉中元组类型表示 (T1, T2, ...)。
 */
class ConeTupleType(
    val elementTypes: List<ConeCangJieType>,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {
    /**
     * 元组是结构化类型，元素类型本身就是它的组成部分。
     */
    override val typeArguments: List<ConeTypeProjection>
        get() = elementTypes.map(::ConeTypeProjection)

    override fun equals(other: Any?): Boolean =
        other is ConeTupleType && elementTypes == other.elementTypes

    override fun hashCode(): Int = elementTypes.hashCode()

}
