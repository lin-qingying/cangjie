package org.cangnova.cangjie.cfir.types

/**
 * 占位类型。
 *
 * 用于在类型尚未确定、但已经需要稳定身份标识时占据类型图位置。
 * 典型场景包括：
 * - 新鲜类型变量的过渡表示
 * - 尚未求解完成的泛型参数
 * - 中层推断阶段的临时占位
 */
class ConePlaceholderType(
    val id: ConeTypeVariableId,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConePlaceholderType) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Placeholder(${id.name}#${id.freshId})"
}

/**
 * 延迟类型。
 *
 * 表示该位置的类型需要等待后续约束传播或延迟分析完成后才能稳定确定。
 */
class ConeDeferredType(
    val id: ConeTypeVariableId,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeDeferredType) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Deferred(${id.name}#${id.freshId})"
}
