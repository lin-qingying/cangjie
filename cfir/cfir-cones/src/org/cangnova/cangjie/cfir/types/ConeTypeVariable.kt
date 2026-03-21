package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.SimpleTypeMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker
import org.cangnova.cangjie.type.model.TypeVariableTypeConstructorMarker

/**
 * 类型变量，类型推断中引入的未知类型。
 *
 * 对应 K2 中的 ConeTypeVariable，在约束求解过程中代表一个待确定的类型。
 *
 * @param name 类型变量名称（通常对应泛型形参名）
 */
class ConeTypeVariable(
    val name: String,
) : TypeVariableMarker {

    /** 该类型变量对应的类型构造器 */
    val typeConstructor: ConeTypeVariableTypeConstructor =
        ConeTypeVariableTypeConstructor(name)

    /** 该类型变量的默认类型（推断未完成时使用） */
    val defaultType: ConeTypeVariableType =
        ConeTypeVariableType(typeConstructor)

    override fun toString(): String = "TypeVariable($name)"
}

/**
 * 类型变量的类型构造器。
 *
 * 在约束系统中标识一个类型变量，使其可与其他类型构造器统一处理。
 */
class ConeTypeVariableTypeConstructor(
    override val name: String,
) : ConeLookupTag(), TypeVariableTypeConstructorMarker {

    override fun equals(other: Any?): Boolean =
        this === other // 类型变量构造器使用引用相等

    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * 类型变量的默认类型。
 *
 * 在推断阶段作为类型变量的占位类型使用，
 * 推断完成后被替换为实际求解结果。
 */
class ConeTypeVariableType(
    val typeVariableConstructor: ConeTypeVariableTypeConstructor,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {

    override val isError: Boolean get() = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeTypeVariableType) return false
        return typeVariableConstructor === other.typeVariableConstructor
    }

    override fun hashCode(): Int = typeVariableConstructor.hashCode()

    override fun toString(): String = typeVariableConstructor.name
}
