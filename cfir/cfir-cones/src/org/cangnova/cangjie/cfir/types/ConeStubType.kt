package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.StubTypeMarker


/**
 * 存根类型，推断过程中的过渡类型。
 *
 * 它用于表达“某个类型位置暂时不能稳定落为最终类型，但已经需要在类型图中占位”。
 * 与 [ConePlaceholderType] 的区别在于：
 * - `ConePlaceholderType` 主要强调稳定身份；
 * - `ConeStubType` 主要强调该位置仍处在推断过渡阶段。
 *
 * 当前 `Kind` 仍保留旧枚举形状，但后续应继续收敛，不再服务 Kotlin Builder 推断语义。
 *
 * @param constructor 关联的类型变量构造器
 * @param kind 存根类型种类
 */
class ConeStubType(
    val constructor: ConeTypeVariableTypeConstructor,
    val kind: Kind,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType()  , StubTypeMarker {

    /** 存根类型种类 */
    enum class Kind {
        /** 用于子类型相关推断过渡 */
        FOR_SUBTYPING,
        /** 保留的过渡分支，后续将重新命名/整理职责 */
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
