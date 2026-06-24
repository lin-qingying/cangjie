package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId

/**
 * 指向错误 class-like 类型的 lookup tag。
 *
 * @property classId 错误类型对外暴露的占位 ClassId。
 * @property diagnostic 该错误类型携带的结构化诊断。
 * @property delegatedType 与该错误类型相关的原始类型，例如未推断出的类型参数类型。
 */
class ConeClassLikeErrorLookupTag(
    override val classId: ClassId,
    val diagnostic: ConeDiagnostic,
    /**
     * A type the error type is somehow related to, e.g., a type parameter type that is uninferred.
     */
    val delegatedType: ConeCangJieType? = null,
) : ConeClassLikeLookupTag()

/**
 * 错误类型，表示类型解析失败。
 * 对应仓颉编译器中的 InvalidTy。
 *
 * @property isUninferredParameter 是否表示未推断出的类型参数。
 * @property typeArguments 错误类型保留的类型实参。
 * @property attributes 错误类型附带的属性。
 * @property lookupTag 错误类型的 lookup tag，保存诊断和委托类型。
 */
class ConeErrorType(
    diagnostic: ConeDiagnostic,
    val isUninferredParameter: Boolean = false,
    delegatedType: ConeCangJieType? = null,
    override val typeArguments: List<  ConeTypeProjection> = emptyList(),
    override val attributes: ConeAttributes = ConeAttributes.Empty,

    override val lookupTag: ConeClassLikeErrorLookupTag =
        ConeClassLikeErrorLookupTag(delegatedType?.classId ?: ClassId.fromString("<error>"), diagnostic, delegatedType)
)  : ConeClassifierType() {

    /**
     * 错误类型携带的结构化诊断。
     */
    val diagnostic: ConeDiagnostic get() = lookupTag.diagnostic

    /**
     * 错误类型关联的原始类型。
     */
    val delegatedType: ConeCangJieType? get() = lookupTag.delegatedType

    /**
     * 错误类型固定返回 `true`。
     */
    override val isError: Boolean get() = true

    /**
     * 错误类型使用引用相等，避免不同错误节点因占位 ClassId 相同而合并。
     */
    override fun equals(other: Any?): Boolean = this === other

    /**
     * 与引用相等匹配的 identity hash。
     */
    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * 不确定类型，对应仓颉编译器中的 QuestTy。
 * 用于不确定的返回类型注解等场景。
 *
 * @property attributes 不确定类型携带的属性。
 */
class ConeQuestType(
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {

    /**
     * 所有 Quest 类型在结构上等价。
     */
    override fun equals(other: Any?): Boolean = other is ConeQuestType

    /**
     * Quest 类型的稳定哈希。
     */
    override fun hashCode(): Int = "Quest".hashCode()

}
