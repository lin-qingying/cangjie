package org.cangnova.cangjie.cfir.types

enum class ConeCaptureStatus {
    FOR_SUBTYPING,
    FOR_INCORPORATION,
    FROM_EXPRESSION,
}

/**
 * 捕获类型，类型推断过程中由泛型实参捕获产生的中间类型。
 *
 * 仓颉没有 Kotlin 那种通配符投影语义，因此这里的捕获类型
 * 不再被视为“对 K2 captured type 的直接镜像”，而是编译器内部
 * 在约束传播阶段使用的过渡类型。
 *
 * @param constructor 该捕获类型的构造器
 * @param lowerType 捕获类型的下界（可为 null）
 * @param status 捕获状态（子类型检查 / 约束合并 / 表达式推断）
 * @param typeParameter 关联的类型参数（如有）
 */
class ConeCapturedType(
    val constructor: ConeCapturedTypeConstructor,
    val lowerType: ConeCangJieType?,
    val status: ConeCaptureStatus,
    val typeParameter: ConeTypeParameterLookupTag? = null,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType()  {

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
 * 持有产生此捕获的原始投影类型和超类型列表。
 *
 * @param projection 产生此捕获的原始投影类型
 * @param supertypes 捕获类型的超类型列表
 */
class ConeCapturedTypeConstructor(
    val projection: ConeCangJieType,
    val supertypes: List<ConeCangJieType>,
) : ConeLookupTag()  {

    override val name: String get() = "CapturedType($projection)"

    override fun equals(other: Any?): Boolean =
        this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}
