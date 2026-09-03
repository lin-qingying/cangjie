package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeContext
import org.cangnova.cangjie.cfir.types.abbreviatedType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.createTypeSubstitutorByTypeConstructor
import org.cangnova.cangjie.cfir.types.declaredUpperBoundConeTypeOrNull
import org.cangnova.cangjie.cfir.types.declaredUpperBoundRefsAfterTypeResolve
import org.cangnova.cangjie.cfir.types.hasInvalidDeclaredUpperBounds
import org.cangnova.cangjie.cfir.types.isLegalDeclaredUpperBound
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceElementOffsetStrategy
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 泛型实参上界检查的共享实现。
 *
 * 对齐 Kotlin FIR `FirUpperBoundViolatedHelpers`：类型使用处和限定访问表达式
 * 共享同一套 upper-bound 校验，只由调用方提供声明侧类型参数、实参和使用点替换。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkUpperBoundViolated(
    typeRef: CfirResolvedTypeRef,
    isIgnoreTypeParameters: Boolean = false,
    useTypeArgumentStartSource: Boolean = false,
    reportDiagnostics: Boolean = true,
): Boolean {
    if (typeRef is CfirErrorTypeRef) return false
    val coneType = typeRef.coneType
    val notExpandedType = coneType.abbreviatedType as? ConeTypeAliasType ?: coneType as? ConeTypeAliasType
    if (notExpandedType != null) {
        return checkUpperBoundViolatedForTypealiasExpansion(
            notExpandedType = notExpandedType,
            fallbackSource = typeRef.source ?: typeRef.delegatedTypeRef?.source,
            useTypeArgumentStartSource = useTypeArgumentStartSource,
            reportDiagnostics = reportDiagnostics,
        )
    }
    return checkUpperBoundViolated(
        type = coneType,
        sourceTypeRef = typeRef.delegatedTypeRef,
        fallbackSource = typeRef.source,
        isIgnoreTypeParameters = isIgnoreTypeParameters,
        useTypeArgumentStartSource = useTypeArgumentStartSource,
        reportDiagnostics = reportDiagnostics,
    )
}

/**
 * 查询类型引用是否包含不能继续参与后续表达式语义的非法泛型实例化。
 *
 * 这与 [checkUpperBoundViolated] 的报告职责分离：上界检查器仍负责产生唯一的
 * `GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT`，表达式检查器只消费这个结果来阻断
 * `REF_NOT_BE_TYPE`、裸 classifier 和 interface static completeness 等级联诊断。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun CfirTypeRef.hasInvalidGenericTypeArgument(): Boolean {
    if (this is CfirErrorTypeRef) return true
    val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return false
    if (resolvedTypeRef.coneType is ConeErrorType) return true
    val result = checkUpperBoundViolated(
        typeRef = resolvedTypeRef,
        reportDiagnostics = false,
    )
    return result
}

/**
 * 检查一个已解析类型在使用点上的泛型实参是否满足声明上界。
 *
 * 调用方可以提供原始 source type ref，用于把诊断精确落到用户写出的类型实参；
 * 当 source 不可用时使用 fallback source 作为兜底位置。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkUpperBoundViolated(
    type: ConeCangJieType,
    sourceTypeRef: CfirTypeRef?,
    fallbackSource: CjSourceElement?,
    isIgnoreTypeParameters: Boolean = false,
    useTypeArgumentStartSource: Boolean = false,
    reportDiagnostics: Boolean = true,
): Boolean {
    val expandedType = (type as? ConeClassifierType)
        ?.fullyExpandedType(context.session) as? ConeClassifierType
        ?: return false
    if (expandedType.typeArguments.isEmpty()) return false

    val symbol = expandedType.toSymbol(context.session) as? CfirClassLikeSymbol<*> ?: return false
    val typeParameters = symbol.cfir.typeParameters
    if (typeParameters.isEmpty()) return false

    val sourceArguments = sourceTypeRef.originalUserTypeRef()
        ?.qualifier
        ?.lastOrNull()
        ?.typeArguments
        .orEmpty()
    val canMapArgumentSources = sourceArguments.size == expandedType.typeArguments.size
    val genericSource = sourceTypeRef?.source ?: fallbackSource
    val substitutor = createGenericUseSiteSubstitutor(
        typeParameters = typeParameters.take(minOf(typeParameters.size, expandedType.typeArguments.size)),
        resolvedArguments = expandedType.typeArguments.map { it.type },
        typeContext = context.session.typeContext,
    )

    return checkUpperBoundViolated(
        typeParameters = typeParameters,
        argumentTypes = expandedType.typeArguments.map { it.type },
        argumentSources = expandedType.typeArguments.indices.map { index ->
            sourceArguments.getOrNull(index)
                .takeIf { canMapArgumentSources }
                ?.source
                ?: genericSource?.firstCharacterDiagnosticSource()
        },
        sourceTypeRefs = sourceArguments.takeIf { canMapArgumentSources },
        fallbackSource = genericSource,
        substitutor = substitutor,
        diagnosticGenericType = expandedType,
        isIgnoreTypeParameters = isIgnoreTypeParameters,
        useTypeArgumentStartSource = useTypeArgumentStartSource,
        reportDiagnostics = reportDiagnostics,
    )
}

/**
 * 检查类型别名展开后的实际泛型类型是否违反上界。
 *
 * 诊断中仍保留未展开的 typealias 类型作为展示对象，避免把用户写出的别名
 * 直接替换成内部展开类型。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkUpperBoundViolatedForTypealiasExpansion(
    notExpandedType: ConeTypeAliasType,
    fallbackSource: CjSourceElement?,
    useTypeArgumentStartSource: Boolean = false,
    reportDiagnostics: Boolean = true,
): Boolean {
    val expandedType = notExpandedType.fullyExpandedType(context.session) as? ConeClassifierType
        ?: return false
    if (expandedType.typeArguments.isEmpty()) return false

    val symbol = expandedType.toSymbol(context.session) as? CfirClassLikeSymbol<*> ?: return false
    val typeParameters = symbol.cfir.typeParameters
    if (typeParameters.isEmpty()) return false

    val substitutor = createGenericUseSiteSubstitutor(
        typeParameters = typeParameters.take(minOf(typeParameters.size, expandedType.typeArguments.size)),
        resolvedArguments = expandedType.typeArguments.map { it.type },
        typeContext = context.session.typeContext,
    )

    return checkUpperBoundViolated(
        typeParameters = typeParameters,
        argumentTypes = expandedType.typeArguments.map { it.type },
        argumentSources = expandedType.typeArguments.indices.map {
            fallbackSource?.firstCharacterDiagnosticSource()
        },
        sourceTypeRefs = null,
        fallbackSource = fallbackSource,
        substitutor = substitutor,
        diagnosticGenericType = notExpandedType,
        useTypeArgumentStartSource = useTypeArgumentStartSource,
        reportDiagnostics = reportDiagnostics,
    )
}

/**
 * 按声明侧类型参数、源码 type argument refs 和调用方提供的替换器检查上界。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkUpperBoundViolated(
    typeParameters: List<CfirTypeParameterRef>,
    typeArgumentRefs: List<CfirTypeRef>,
    substitutor: ConeSubstitutor,
    fallbackSource: CjSourceElement?,
) {
    checkUpperBoundViolated(
        typeParameters = typeParameters,
        argumentTypes = typeArgumentRefs.map { it.coneTypeOrNull },
        argumentSources = typeArgumentRefs.map { it.source },
        sourceTypeRefs = typeArgumentRefs,
        fallbackSource = fallbackSource,
        substitutor = substitutor,
        diagnosticGenericType = null,
    )
}

/**
 * 为泛型使用点创建类型参数到实际类型实参的替换器。
 *
 * [additionalSubstitutions] 用于调用方先注入外层声明或类型别名展开时已经确定的替换。
 */
internal fun createGenericUseSiteSubstitutor(
    typeParameters: List<CfirTypeParameterRef>,
    resolvedArguments: List<ConeCangJieType>,
    typeContext: ConeTypeContext,
    additionalSubstitutions: Map<TypeConstructorMarker, ConeCangJieType> = emptyMap(),
): ConeSubstitutor {
    val substitutions = LinkedHashMap<TypeConstructorMarker, ConeCangJieType>(additionalSubstitutions)
    typeParameters.zip(resolvedArguments).forEach { (typeParameter, argument) ->
        substitutions[typeParameter.symbol.toLookupTag() as TypeConstructorMarker] = argument
    }
    return createTypeSubstitutorByTypeConstructor(
        map = substitutions,
        context = typeContext,
        approximateIntegerLiterals = false,
    )
}

/**
 * 泛型上界检查的核心实现。
 *
 * 该函数同时处理当前层实参的上界验证，以及嵌套泛型实参的递归验证；
 * 诊断位置按源码 type argument、泛型整体 source、fallback source 的顺序选择。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkUpperBoundViolated(
    typeParameters: List<CfirTypeParameterRef>,
    argumentTypes: List<ConeCangJieType?>,
    argumentSources: List<CjSourceElement?>,
    sourceTypeRefs: List<CfirTypeRef>?,
    fallbackSource: CjSourceElement?,
    substitutor: ConeSubstitutor,
    diagnosticGenericType: ConeCangJieType?,
    isIgnoreTypeParameters: Boolean = false,
    useTypeArgumentStartSource: Boolean = false,
    reportDiagnostics: Boolean = true,
): Boolean {
    var hasInvalidArgument = false
    val count = minOf(typeParameters.size, argumentTypes.size)
    for (index in 0 until count) {
        val argumentType = argumentTypes[index] ?: continue
        val sourceTypeRef = sourceTypeRefs?.getOrNull(index)
        val argumentSource = argumentSources.getOrNull(index)
            ?.let { source ->
                if (useTypeArgumentStartSource) source.firstCharacterDiagnosticSource() else source
            }
            ?: fallbackSource

        val currentUpperBound: ConeCangJieType? = if (
            argumentType !is ConeErrorType &&
            !argumentType.isGenericTypeWithInvalidUpperBound() &&
            (!isIgnoreTypeParameters || (argumentType.typeArguments.isEmpty() && argumentType !is ConeTypeParameterType))
        ) {
            val upperBounds = typeParameters[index].declaredUpperBoundTypes()
            if (upperBounds.isNotEmpty()) {
                val substitutedUpperBound = substitutor.substituteOrSelf(
                    context.session.typeContext.intersectTypes(upperBounds) as ConeCangJieType,
                ) as ConeCangJieType
                if (substitutedUpperBound is ConeErrorType) null else substitutedUpperBound
            } else {
                null
            }
        } else {
            null
        }
        val violatesCurrentUpperBound = currentUpperBound != null &&
            !AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(
                context.session.typeContext,
                argumentType,
                currentUpperBound,
            )

        val hasInvalidNestedArgument = if (sourceTypeRef == null && argumentSource == null) {
            false
        } else {
            checkUpperBoundViolated(
                type = argumentType,
                sourceTypeRef = sourceTypeRef,
                fallbackSource = argumentSource,
                isIgnoreTypeParameters = isIgnoreTypeParameters,
                useTypeArgumentStartSource = useTypeArgumentStartSource,
                reportDiagnostics = reportDiagnostics,
            )
        }

        /**
         * 官方 `CheckUpperBoundsLegalityRecursively` 在发现更深层的非法实例化后，
         * 只保留最深层实参诊断；当前层由声明级诊断覆盖，不能再重复标记外层泛型名。
         */
        if (reportDiagnostics && violatesCurrentUpperBound && !hasInvalidNestedArgument) {
            if (argumentSource != null) {
                reporter.reportOn(
                    source = argumentSource,
                    factory = CfirErrors.GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT,
                    a = argumentType,
                    b = currentUpperBound,
                    c = diagnosticGenericType ?: typeParameters[index].symbol.constructType(),
                )
            }
        }
        hasInvalidArgument = hasInvalidArgument || violatesCurrentUpperBound || hasInvalidNestedArgument
    }
    return hasInvalidArgument
}

/**
 * 判断类型参数自身是否携带非法上界。
 *
 * 当实参对应的类型参数定义已经有非法上界时，使用点不再重复报告普通上界不匹配。
 */
context(context: CheckerContext)
private fun ConeCangJieType.isGenericTypeWithInvalidUpperBound(): Boolean {
    val typeParameterType = this as? ConeTypeParameterType ?: return false
    return typeParameterType.hasInvalidDeclaredUpperBounds(context.session)
}

context(context: CheckerContext)
private fun CfirTypeParameterRef.hasInvalidDeclaredUpperBounds(): Boolean =
    symbol.toLookupTag().hasInvalidDeclaredUpperBounds(context.session)

context(context: CheckerContext)
private fun CfirTypeParameterRef.declaredUpperBoundTypes(): List<ConeCangJieType> {
    val bounds = symbol.toLookupTag()
        .declaredUpperBoundRefsAfterTypeResolve()
        .mapNotNull { it.declaredUpperBoundConeTypeOrNull() }
        .filterNot { it is ConeErrorType }
    return bounds.filter { it.isLegalDeclaredUpperBound(context.session) }
        .ifEmpty { bounds }
}

/**
 * 从 resolved type ref 链中还原用户源码写出的 type ref。
 */
private fun CfirTypeRef?.originalUserTypeRef(): CfirUserTypeRef? =
    when (this) {
        is CfirUserTypeRef -> this
        is CfirResolvedTypeRef -> delegatedTypeRef.originalUserTypeRef()
        else -> null
    }

/**
 * 为类型引用诊断创建只覆盖首字符的 source。
 *
 * 上界错误通常应该定位到具体类型实参起点，而不是整个复杂 type ref。
 */
internal fun CjSourceElement.firstCharacterDiagnosticSource(): CjSourceElement =
    fakeElement(
        CjFakeSourceElementKind.ErrorTypeRef,
        CjSourceElementOffsetStrategy.Custom.Initialized(
            startOffset = startOffset,
            endOffset = (startOffset + 1).coerceAtMost(endOffset),
        ),
    )
