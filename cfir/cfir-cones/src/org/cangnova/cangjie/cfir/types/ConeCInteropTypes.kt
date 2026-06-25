package org.cangnova.cangjie.cfir.types

/**
 * CPointer 指针类型，对应仓颉编译器中的 PointerTy。
 * 用于 C 互操作。
 *
 * @property pointeeType 指针指向的元素类型。
 * @property attributes 指针类型附带的属性。
 */
class ConePointerType(
    /** 指针指向的元素类型。 */
    val pointeeType: ConeCangJieType,
    /** 指针类型附带的属性。 */
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {

    /**
     * CPointer 类型按指向类型判等。
     */
    override fun equals(other: Any?): Boolean =
        other is ConePointerType && pointeeType == other.pointeeType

    /**
     * CPointer 类型的结构哈希。
     */
    override fun hashCode(): Int = pointeeType.hashCode()

}

/**
 * CString 类型，对应仓颉编译器中的 CStringTy。
 * 用于 C 互操作的 char* 字符串。
 *
 * @property attributes CString 类型附带的属性。
 */
class ConeCStringType(
    /** CString 类型附带的属性。 */
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {

    /**
     * 所有 CString 类型在结构上等价。
     */
    override fun equals(other: Any?): Boolean = other is ConeCStringType

    /**
     * CString 类型的稳定哈希。
     */
    override fun hashCode(): Int = "CString".hashCode()

}

/**
 * Any 顶类型，对应仓颉编译器中的 AnyTy。
 * 类型检查期间内部使用的临时顶类型。
 */
object ConeAnyType : ConeRigidType(), ConeTypeConstructorMarker {
    /**
     * Any 顶类型没有附加属性。
     */
    override val attributes: ConeAttributes get() = ConeAttributes.Empty

    /**
     * Any 顶类型使用对象身份判等。
     */
    override fun equals(other: Any?): Boolean = this === other

    /**
     * 与对象身份判等匹配的 identity hash。
     */
    override fun hashCode(): Int = System.identityHashCode(this)
}
