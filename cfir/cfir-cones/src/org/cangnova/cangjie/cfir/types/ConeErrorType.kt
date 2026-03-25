package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId

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
 */
class ConeErrorType(
    diagnostic: ConeDiagnostic,
    val isUninferredParameter: Boolean = false,
    delegatedType: ConeCangJieType? = null,
    override val typeArguments: List<  ConeTypeProjection> = emptyList(),
    override val attributes: ConeAttributes = ConeAttributes.Empty,
    val nullable: Boolean? = null,
    override val lookupTag: ConeClassLikeErrorLookupTag =
        ConeClassLikeErrorLookupTag(delegatedType?.classId ?: ClassId.fromString("<error>"), diagnostic, delegatedType)
)  : ConeClassifierType() {

    val diagnostic: ConeDiagnostic get() = lookupTag.diagnostic
    val delegatedType: ConeCangJieType? get() = lookupTag.delegatedType

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}
/**
 * 不确定类型，对应仓颉编译器中的 QuestTy。
 * 用于不确定的返回类型注解等场景。
 */
class ConeQuestType(
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {

    override fun equals(other: Any?): Boolean = other is ConeQuestType

    override fun hashCode(): Int = "Quest".hashCode()

    override fun toString(): String = "?"
}
