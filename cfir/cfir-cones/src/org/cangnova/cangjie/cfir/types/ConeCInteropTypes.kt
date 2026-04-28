package org.cangnova.cangjie.cfir.types

/**
 * CPointer 指针类型，对应仓颉编译器中的 PointerTy。
 * 用于 C 互操作。
 */
class ConePointerType(
    val pointeeType: ConeCangJieType,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {

    override fun equals(other: Any?): Boolean =
        other is ConePointerType && pointeeType == other.pointeeType

    override fun hashCode(): Int = pointeeType.hashCode()

}

/**
 * CString 类型，对应仓颉编译器中的 CStringTy。
 * 用于 C 互操作的 char* 字符串。
 */
class ConeCStringType(
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {

    override fun equals(other: Any?): Boolean = other is ConeCStringType

    override fun hashCode(): Int = "CString".hashCode()

}

/**
 * Any 顶类型，对应仓颉编译器中的 AnyTy。
 * 类型检查期间内部使用的临时顶类型。
 */
object ConeAnyType : ConeRigidType(), ConeTypeConstructorMarker {
    override val attributes: ConeAttributes get() = ConeAttributes.Empty

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}
