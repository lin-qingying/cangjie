package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.resolve.fullyExpandedTypeUsingAbbreviation
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierAmbiguityDiagnostic
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeRecoverableNominalDiagnostic
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement

/**
 * 声明头中单个父类型的结构化分类。
 *
 * 该模型是 checker、继承环、类型系统父类型提供器和成员作用域的共同语义边界。
 * 不同消费者必须显式选择自己的投影：只有 [ValidNominal] 是普通父类型并提供继承成员；
 * [RecoverableNominalError] 仍能提供继承环与 final-class 检查所需的 owner，但不能进入
 * 普通类型关系、构造器依赖或成员作用域；[LoopError] 只保留独立声明检查所需的信息。
 */
sealed interface DeclaredSupertypeClassification {
    /** 已解析为合法 class/interface 的普通声明父类型。 */
    data class ValidNominal(val type: ConeClassLikeType) : DeclaredSupertypeClassification

    /** 已经证明目标不是 class/interface，或 classifier 本身存在声明歧义。 */
    data class InvalidTargetKind(val type: ConeCangJieType?) : DeclaredSupertypeClassification

    /**
     * 类型构造器已经解析为 class/interface，但声明父类型整体无效。
     *
     * 该分类只参与继承环与 nominal 声明规则，不构成普通类型关系、构造器依赖，
     * 也不提供继承成员。
     */
    data class RecoverableNominalError(
        val errorType: ConeErrorType,
        val nominalType: ConeClassLikeType,
    ) : DeclaredSupertypeClassification

    /** 名称、类型位置或其他主解析错误；不得派生声明父类型诊断。 */
    data class PrimaryResolutionError(val errorType: ConeErrorType?) : DeclaredSupertypeClassification

    /**
     * 继承环错误。broken edge 不进入图或成员作用域，但 delegated nominal 类型仍供
     * NON_INHERITABLE 和递归构造调用等独立检查使用。
     */
    data class LoopError(
        val errorType: ConeErrorType,
        val delegatedNominalType: ConeClassLikeType?,
    ) : DeclaredSupertypeClassification
}

/**
 * 对声明父类型引用进行结构化分类。
 *
 * [expandType] 允许 SUPER_TYPES 计算会话使用尚未写回声明树的 typealias 展开结果；
 * 其他阶段默认按 use-site abbreviation 完整展开 alias。
 */
fun CfirTypeRef.classifyDeclaredSupertype(
    session: CfirSession,
    expandType: (ConeCangJieType) -> ConeCangJieType = {
        it.fullyExpandedTypeUsingAbbreviation(session)
    },
): DeclaredSupertypeClassification = classifyDeclaredSupertype(
    expandType = expandType,
    visitedTypeRefs = linkedSetOf(),
)

/**
 * 使用调用方提供的类型展开策略分类父类型。
 *
 * SUPER_TYPES supplier 在尚未持有完整 session 展开上下文时使用 identity 策略，
 * 因而只发布已经明确解析成 [ConeClassLikeType] 的父类型。
 */
fun CfirTypeRef.classifyDeclaredSupertype(
    expandType: (ConeCangJieType) -> ConeCangJieType,
): DeclaredSupertypeClassification = classifyDeclaredSupertype(
    expandType = expandType,
    visitedTypeRefs = linkedSetOf(),
)

private fun CfirTypeRef.classifyDeclaredSupertype(
    expandType: (ConeCangJieType) -> ConeCangJieType,
    visitedTypeRefs: MutableSet<CfirTypeRef>,
): DeclaredSupertypeClassification {
    if (!visitedTypeRefs.add(this)) {
        return DeclaredSupertypeClassification.PrimaryResolutionError(errorTypeOrNull())
    }

    val resolvedTypeRef = this as? CfirResolvedTypeRef
        ?: return DeclaredSupertypeClassification.PrimaryResolutionError(null)
    val errorType = resolvedTypeRef.coneType as? ConeErrorType
    if (errorType == null) {
        return classifyResolvedDeclaredSupertype(expandType(resolvedTypeRef.coneType))
    }

    val diagnostic = (resolvedTypeRef as? CfirErrorTypeRef)?.diagnostic ?: errorType.diagnostic
    if (diagnostic is ConeSimpleDiagnostic && diagnostic.kind.isDeclaredSupertypeLoopKind()) {
        val delegatedNominal = resolvedTypeRef.delegatedTypeRef
            ?.classifyDeclaredSupertype(expandType, visitedTypeRefs)
            .nominalTypeForLoopCheckOrNull()
        return DeclaredSupertypeClassification.LoopError(errorType, delegatedNominal)
    }

    if (diagnostic is ConeClassifierAmbiguityDiagnostic) {
        return DeclaredSupertypeClassification.InvalidTargetKind(errorType)
    }

    if (diagnostic is ConeRecoverableNominalDiagnostic) {
        val delegatedType = errorType.delegatedType?.let(expandType)
        if (delegatedType is ConeClassLikeType) {
            return DeclaredSupertypeClassification.RecoverableNominalError(errorType, delegatedType)
        }
        return DeclaredSupertypeClassification.PrimaryResolutionError(errorType)
    }

    // resolve 阶段会把直接父类型参数包装成错误 typeRef，并保留其 resolved delegated ref。
    // 只有 delegated 结构明确证明目标种类非法时才派生 InvalidTargetKind；未知错误保持主错误。
    val delegatedClassification = resolvedTypeRef.delegatedTypeRef
        ?.classifyDeclaredSupertype(expandType, visitedTypeRefs)
    return if (delegatedClassification is DeclaredSupertypeClassification.InvalidTargetKind) {
        delegatedClassification
    } else {
        DeclaredSupertypeClassification.PrimaryResolutionError(errorType)
    }
}

private fun classifyResolvedDeclaredSupertype(type: ConeCangJieType): DeclaredSupertypeClassification =
    when (type) {
        is ConeClassLikeType -> DeclaredSupertypeClassification.ValidNominal(type)
        is ConeErrorType -> DeclaredSupertypeClassification.PrimaryResolutionError(type)
        is ConeTypeParameterType -> DeclaredSupertypeClassification.InvalidTargetKind(type)
        else -> DeclaredSupertypeClassification.InvalidTargetKind(type)
    }

private fun CfirTypeRef.errorTypeOrNull(): ConeErrorType? =
    (this as? CfirResolvedTypeRef)?.coneType as? ConeErrorType

private fun DiagnosticKind.isDeclaredSupertypeLoopKind(): Boolean =
    this == DiagnosticKind.LoopInSupertype || this == DiagnosticKind.SupertypeSelfReference

private fun DeclaredSupertypeClassification?.nominalTypeForLoopCheckOrNull(): ConeClassLikeType? = when (this) {
    is DeclaredSupertypeClassification.ValidNominal -> type
    is DeclaredSupertypeClassification.RecoverableNominalError -> nominalType
    is DeclaredSupertypeClassification.LoopError -> delegatedNominalType
    else -> null
}

/** 仅返回能够进入普通类型关系的合法 nominal 父类型。 */
fun DeclaredSupertypeClassification.ordinarySupertypeTypeOrNull(): ConeClassLikeType? =
    (this as? DeclaredSupertypeClassification.ValidNominal)?.type

/**
 * 返回继承环 DFS 能够追踪的声明目标。
 *
 * 泛型实参数量错误仍保留官方继承环检查所需的 class owner，但该投影不得被类型系统、
 * 构造器依赖或成员作用域复用。
 */
fun DeclaredSupertypeClassification.inheritanceCycleDependencyTypeOrNull(): ConeClassLikeType? = when (this) {
    is DeclaredSupertypeClassification.ValidNominal -> type
    is DeclaredSupertypeClassification.RecoverableNominalError -> nominalType
    else -> null
}

/** 返回 final、父类型顺序等独立声明规则能够检查的 nominal owner。 */
fun DeclaredSupertypeClassification.independentNominalCheckTypeOrNull(): ConeClassLikeType? = when (this) {
    is DeclaredSupertypeClassification.ValidNominal -> type
    is DeclaredSupertypeClassification.RecoverableNominalError -> nominalType
    is DeclaredSupertypeClassification.LoopError -> delegatedNominalType
    else -> null
}

/**
 * 返回构造器委托依赖可以使用的 concrete 父类型视图。
 *
 * 普通构造器依赖只接受合法父边；循环构造诊断显式请求时，才允许读取已经标记为
 * [LoopError] 的 delegated nominal。可恢复的泛型实参数量错误始终不属于构造器依赖。
 */
fun DeclaredSupertypeClassification.constructorDependencyTypeOrNull(
    includeLoopError: Boolean,
): ConeClassLikeType? = when (this) {
    is DeclaredSupertypeClassification.ValidNominal -> type
    is DeclaredSupertypeClassification.LoopError -> delegatedNominalType.takeIf { includeLoopError }
    else -> null
}

/** 仅合法声明父边能够提供继承成员。 */
fun DeclaredSupertypeClassification.scopeTraversalTypeOrNull(): ConeClassLikeType? =
    (this as? DeclaredSupertypeClassification.ValidNominal)?.type

/** 返回继承环背后的 nominal 类型，供独立的父类修饰符检查使用。 */
fun DeclaredSupertypeClassification.loopDelegatedNominalTypeOrNull(): ConeClassLikeType? =
    (this as? DeclaredSupertypeClassification.LoopError)?.delegatedNominalType

/**
 * 取得父类型最外层 classifier 名称的完整 token 范围。
 *
 * resolve 可能叠加 error/resolved typeRef；必须递归回到原始 [CfirUserTypeRef]，不能把
 * 继承环为声明级诊断保存的 owner source 误用于父类型分类诊断。
 */
fun CfirTypeRef.declaredSupertypeClassifierSource(): AbstractCjSourceElement? {
    val userTypeRef = originalDeclaredSupertypeUserTypeRef(linkedSetOf()) ?: return source
    val qualifierPart = userTypeRef.qualifier.lastOrNull() ?: return source
    val qualifierSource = qualifierPart.source ?: return source
    return CjOffsetsOnlySourceElement(
        startOffset = qualifierSource.startOffset,
        endOffset = (qualifierSource.startOffset + qualifierPart.name.asString().length)
            .coerceAtMost(qualifierSource.endOffset),
    )
}

private fun CfirTypeRef.originalDeclaredSupertypeUserTypeRef(
    visited: MutableSet<CfirTypeRef>,
): CfirUserTypeRef? {
    if (!visited.add(this)) return null
    return when (this) {
        is CfirUserTypeRef -> this
        is CfirErrorTypeRef -> delegatedTypeRef?.originalDeclaredSupertypeUserTypeRef(visited)
            ?: partiallyResolvedTypeRef?.originalDeclaredSupertypeUserTypeRef(visited)
        is CfirResolvedTypeRef -> delegatedTypeRef?.originalDeclaredSupertypeUserTypeRef(visited)
        else -> null
    }
}
