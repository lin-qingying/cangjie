package org.cangjie.cfir.types

/**
 * 元组类型。
 * 仓颉中元组类型表示 (T1, T2, ...)。
 */
class ConeTupleType(
    val elementTypes: List<ConeCangjieType>,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {

    override fun equals(other: Any?): Boolean =
        other is ConeTupleType && elementTypes == other.elementTypes

    override fun hashCode(): Int = elementTypes.hashCode()

    override fun toString(): String =
        elementTypes.joinToString(prefix = "(", postfix = ")")
}
