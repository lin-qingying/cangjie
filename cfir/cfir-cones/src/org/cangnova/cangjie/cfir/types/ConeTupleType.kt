package org.cangnova.cangjie.cfir.types

/**
 * 元组类型。
 * 仓颉中元组类型表示 (T1, T2, ...)。
 *
 * @property elementTypes 元组元素类型列表。
 * @property attributes 元组类型附带的属性。
 */
class ConeTupleType(
    /** 元组元素类型列表。 */
    val elementTypes: List<ConeCangJieType>,
    /** 元组类型附带的属性。 */
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {
    /**
     * 元组类型按元素类型列表判等。
     */
    override fun equals(other: Any?): Boolean =
        other is ConeTupleType && elementTypes == other.elementTypes

    /**
     * 元组类型的结构哈希。
     */
    override fun hashCode(): Int = elementTypes.hashCode()

}
