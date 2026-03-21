package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.CapturedTypeConstructorMarker
import org.cangnova.cangjie.type.model.CapturedTypeMarker
import org.cangnova.cangjie.type.model.CaptureStatus
import org.cangnova.cangjie.type.model.TypeArgumentMarker
import org.cangnova.cangjie.type.model.TypeParameterMarker

/**
 * 捕获类型，类型推断过程中由泛型实参捕获产生的中间类型。
 *
 * 对应 K2 中的 ConeCapturedType。仓颉无通配符投影，
 * 捕获类型仅在编译器内部约束传播阶段使用。
 *
 * @param constructor 该捕获类型的构造器
 * @param lowerType 捕获类型的下界（可为 null）
 * @param status 捕获状态（子类型检查 / 约束合并 / 表达式推断）
 * @param typeParameter 关联的类型参数（如有）
 */
class ConeCapturedType(
    val constructor: ConeCapturedTypeConstructor,
    val lowerType: ConeCangJieType?,
    val status: CaptureStatus,
    val typeParameter: TypeParameterMarker? = null,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType(), CapturedTypeMarker {

    override val isError: Boolean get() = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeCapturedType) return false
        return constructor == other.constructor && status == other.status
    }

    override fun hashCode(): Int = constructor.hashCode() * 31 + status.hashCode()

    override fun toString(): String = "Captured(${constructor.projection})"
}

/**
 * 捕获类型的构造器。
 *
 * 持有产生此捕获的原始泛型实参和超类型列表。
 *
 * @param projection 产生此捕获的泛型实参
 * @param supertypes 捕获类型的超类型列表
 */
class ConeCapturedTypeConstructor(
    val projection: TypeArgumentMarker,
    val supertypes: List<ConeCangJieType>,
) : ConeLookupTag(), CapturedTypeConstructorMarker {

    override val name: String get() = "CapturedType($projection)"

    override fun equals(other: Any?): Boolean =
        this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}
