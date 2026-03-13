package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId

/**
 * 类/接口实例类型，对应仓颉编译器中的 ClassTy / InterfaceTy / ClassThisTy。
 *
 * 使用 isInterface 区分 class 和 interface。
 * 当 isThisType=true 时，表示 ClassThisTy（类内部的 This 类型）。
 */
class ConeClassLikeType(
    val lookupTag: ConeClassLikeLookupTag,
    override val typeArguments: List<ConeCangjieType> = emptyList(),
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
    val isInterface: Boolean = false,
    /** 是否为 This 类型引用（仓颉 class 内部引用自身类型，对应 ClassThisTy） */
    val isThisType: Boolean = false,
) : ConeRigidType() {

    val classId: ClassId get() = lookupTag.classId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeClassLikeType) return false
        return lookupTag == other.lookupTag &&
            typeArguments == other.typeArguments &&
            isThisType == other.isThisType
    }

    override fun hashCode(): Int {
        var result = lookupTag.hashCode()
        result = 31 * result + typeArguments.hashCode()
        return result
    }

    override fun toString(): String = buildString {
        append(lookupTag.name)
        if (typeArguments.isNotEmpty()) {
            typeArguments.joinTo(this, prefix = "<", postfix = ">")
        }
        if (isThisType) append("(This)")
    }
}

/**
 * 结构体类型（值类型），对应仓颉编译器中的 StructTy。
 */
class ConeStructType(
    val lookupTag: ConeClassLikeLookupTag,
    override val typeArguments: List<ConeCangjieType> = emptyList(),
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {

    val classId: ClassId get() = lookupTag.classId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeStructType) return false
        return lookupTag == other.lookupTag && typeArguments == other.typeArguments
    }

    override fun hashCode(): Int {
        var result = lookupTag.hashCode()
        result = 31 * result + typeArguments.hashCode()
        return result
    }

    override fun toString(): String = buildString {
        append("struct ")
        append(lookupTag.name)
        if (typeArguments.isNotEmpty()) {
            typeArguments.joinTo(this, prefix = "<", postfix = ">")
        }
    }
}

/**
 * 枚举类型（代数数据类型），对应仓颉编译器中的 EnumTy / RefEnumTy。
 *
 * 仓颉的 enum 是 ADT，可以有带参数的构造器。
 * isRefEnum 表示是否为引用枚举类型（RefEnumTy）。
 */
class ConeEnumType(
    val lookupTag: ConeClassLikeLookupTag,
    override val typeArguments: List<ConeCangjieType> = emptyList(),
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
    /** 是否为引用枚举类型（RefEnumTy） */
    val isRefEnum: Boolean = false,
) : ConeRigidType() {

    val classId: ClassId get() = lookupTag.classId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeEnumType) return false
        return lookupTag == other.lookupTag &&
            typeArguments == other.typeArguments &&
            isRefEnum == other.isRefEnum
    }

    override fun hashCode(): Int {
        var result = lookupTag.hashCode()
        result = 31 * result + typeArguments.hashCode()
        result = 31 * result + isRefEnum.hashCode()
        return result
    }

    override fun toString(): String = buildString {
        if (isRefEnum) append("ref ")
        append("enum ")
        append(lookupTag.name)
        if (typeArguments.isNotEmpty()) {
            typeArguments.joinTo(this, prefix = "<", postfix = ">")
        }
    }
}
