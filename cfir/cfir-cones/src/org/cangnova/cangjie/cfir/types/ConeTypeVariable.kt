package org.cangnova.cangjie.cfir.types

 /**
 * 类型变量的类型构造器。
 *
 * 在约束系统中标识一个类型变量，使其可与其他类型构造器统一处理。
 */
class ConeTypeVariableTypeConstructor(
    override val name: String,
) : ConeLookupTag() {

    override fun equals(other: Any?): Boolean =
        this === other // 类型变量构造器使用引用相等

    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * 类型变量的过渡兼容类型。
 *
 * 它保留旧有 `ConeTypeVariableType` 的二进制/源码兼容角色，
 * 但新的状态承载应转移到 [ConeTypeVariableState]，
 * 新的类型图引用应优先使用 [ConeTypeVariableRef]。
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
