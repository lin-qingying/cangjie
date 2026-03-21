package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.StubTypeMarker
import org.cangnova.cangjie.type.model.TypeVariableTypeConstructorMarker

/**
 * 存根类型，类型推断中间状态的占位类型。
 *
 * 对应 K2 中的 ConeStubType，在推断尚未完成时临时代表某个类型变量。
 * 推断完成后，所有存根类型将被替换为实际求解结果。
 *
 * @param constructor 关联的类型变量构造器
 * @param kind 存根类型种类（用于子类型检查 / Builder 推断）
 */
class ConeStubType(
    val constructor: ConeTypeVariableTypeConstructor,
    val kind: Kind,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType(), StubTypeMarker {

    /** 存根类型种类 */
    enum class Kind {
        /** 用于子类型检查中的类型变量 */
        FOR_SUBTYPING,
        /** 用于 Builder 推断 */
        FOR_BUILDER_INFERENCE,
    }

    override val isError: Boolean get() = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeStubType) return false
        return constructor === other.constructor && kind == other.kind
    }

    override fun hashCode(): Int = constructor.hashCode() * 31 + kind.hashCode()

    override fun toString(): String = "Stub(${constructor.name}:$kind)"
}
