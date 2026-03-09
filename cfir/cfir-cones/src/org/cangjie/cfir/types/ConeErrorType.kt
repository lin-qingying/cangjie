package org.cangjie.cfir.types

/**
 * 错误类型，表示类型解析失败。
 * 对应仓颉编译器中的 InvalidTy。
 */
class ConeErrorType(
    val reason: String,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {

    override val isError: Boolean get() = true

    override fun equals(other: Any?): Boolean =
        other is ConeErrorType && reason == other.reason

    override fun hashCode(): Int = reason.hashCode()

    override fun toString(): String = "ERROR($reason)"
}

/**
 * 不确定类型，对应仓颉编译器中的 QuestTy。
 * 用于不确定的返回类型注解等场景。
 */
class ConeQuestType(
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {

    override fun equals(other: Any?): Boolean = other is ConeQuestType

    override fun hashCode(): Int = "Quest".hashCode()

    override fun toString(): String = "?"
}
