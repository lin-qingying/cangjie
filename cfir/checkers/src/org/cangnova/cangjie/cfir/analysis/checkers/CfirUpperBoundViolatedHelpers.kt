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
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeContext
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.abbreviatedType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.createTypeSubstitutorByTypeConstructor
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
) {
    if (typeRef is CfirErrorTypeRef) return
    val coneType = typeRef.coneType
    val notExpandedType = coneType.abbreviatedType as? ConeTypeAliasType ?: coneType as? ConeTypeAliasType
    if (notExpandedType != null) {
        checkUpperBoundViolatedForTypealiasExpansion(
            notExpandedType = notExpandedType,
            fallbackSource = typeRef.source ?: typeRef.delegatedTypeRef?.source,
        )
        return
    }
    checkUpperBoundViolated(
        type = coneType,
        sourceTypeRef = typeRef.delegatedTypeRef,
        fallbackSource = typeRef.source,
        isIgnoreTypeParameters = isIgnoreTypeParameters,
    )
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
) {
    val expandedType = (type as? ConeClassifierType)
        ?.fullyExpandedType(context.session) as? ConeClassifierType
        ?: return
    if (expandedType.typeArguments.isEmpty()) return

    val symbol = expandedType.toSymbol(context.session) as? CfirClassLikeSymbol<*> ?: return
    val typeParameters = symbol.cfir.typeParameters
    if (typeParameters.isEmpty()) return

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

    checkUpperBoundViolated(
        typeParameters = typeParameters,
        argumentTypes = expandedType.typeArguments.map { it.type },
        argumentSources = expandedType.typeArguments.indices.map { index ->
            sourceArguments.getOrNull(index)
                .takeIf { canMapArgumentSources }
                ?.source
                ?.firstCharacterDiagnosticSource()
                ?: genericSource?.firstCharacterDiagnosticSource()
        },
        sourceTypeRefs = sourceArguments.takeIf { canMapArgumentSources },
        fallbackSource = genericSource,
        substitutor = substitutor,
        diagnosticGenericType = expandedType,
        isIgnoreTypeParameters = isIgnoreTypeParameters,
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
) {
    val expandedType = notExpandedType.fullyExpandedType(context.session) as? ConeClassifierType
        ?: return
    if (expandedType.typeArguments.isEmpty()) return

    val symbol = expandedType.toSymbol(context.session) as? CfirClassLikeSymbol<*> ?: return
    val typeParameters = symbol.cfir.typeParameters
    if (typeParameters.isEmpty()) return

    val substitutor = createGenericUseSiteSubstitutor(
        typeParameters = typeParameters.take(minOf(typeParameters.size, expandedType.typeArguments.size)),
        resolvedArguments = expandedType.typeArguments.map { it.type },
        typeContext = context.session.typeContext,
    )

    checkUpperBoundViolated(
        typeParameters = typeParameters,
        argumentTypes = expandedType.typeArguments.map { it.type },
        argumentSources = expandedType.typeArguments.indices.map {
            fallbackSource?.firstCharacterDiagnosticSource()
        },
        sourceTypeRefs = null,
        fallbackSource = fallbackSource,
        substitutor = substitutor,
        diagnosticGenericType = notExpandedType,
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
        argumentSources = typeArgumentRefs.map { it.source?.firstCharacterDiagnosticSource() },
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
) {
    val count = minOf(typeParameters.size, argumentTypes.size)
    for (index in 0 until count) {
        val argumentType = argumentTypes[index] ?: continue
        val sourceTypeRef = sourceTypeRefs?.getOrNull(index)
        val argumentSource = argumentSources.getOrNull(index) ?: fallbackSource

        if (
            argumentType !is ConeErrorType &&
            !argumentType.isGenericTypeWithInvalidUpperBound() &&
            (!isIgnoreTypeParameters || (argumentType.typeArguments.isEmpty() && argumentType !is ConeTypeParameterType))
        ) {
            val upperBounds = typeParameters[index].symbol.resolvedBounds
                .map { it.coneType }
                .filterNot { it is ConeErrorType }
            if (upperBounds.isNotEmpty()) {
                val upperBound = substitutor.substituteOrSelf(
                    context.session.typeContext.intersectTypes(upperBounds) as ConeCangJieType,
                )
                if (
                    upperBound !is ConeErrorType &&
                    !AbstractTypeChecker.isSubtypeOf(context.session.typeContext, argumentType, upperBound)
                ) {
                    // 合成声明可能携带无 source 的 resolved type ref；真实源码 type ref 会在递归路径继续检查。
                    if (argumentSource != null) {
                        reporter.reportOn(
                            source = argumentSource,
                            factory = CfirErrors.GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT,
                            a = argumentType,
                            b = upperBound,
                            c = diagnosticGenericType ?: typeParameters[index].symbol.constructType(),
                        )
                    }
                }
            }
        }

        if (sourceTypeRef == null && argumentSource == null) continue
        checkUpperBoundViolated(
            type = argumentType,
            sourceTypeRef = sourceTypeRef,
            fallbackSource = argumentSource,
            isIgnoreTypeParameters = isIgnoreTypeParameters,
        )
    }
}

/**
 * 判断类型参数自身是否携带非法上界。
 *
 * 当实参对应的类型参数定义已经有非法上界时，使用点不再重复报告普通上界不匹配。
 */
context(context: CheckerContext)
private fun ConeCangJieType.isGenericTypeWithInvalidUpperBound(): Boolean {
    val typeParameterType = this as? ConeTypeParameterType ?: return false
    return typeParameterType.lookupTag.typeParameterSymbol.resolvedBounds.any { bound ->
        val boundType = bound.coneType
        if (boundType is ConeErrorType) return@any true
        !boundType.isLegalGenericUpperBound()
    }
}

/**
 * 判断类型是否允许作为泛型类型参数上界。
 */
context(context: CheckerContext)
private fun ConeCangJieType.isLegalGenericUpperBound(): Boolean {
    val expandedType = fullyExpandedType(context.session)
    if (expandedType is ConeClassLikeType || expandedType == ConeAnyType) return true

    val classId = expandedType.classIdOrPrimitiveClassId
    return classId == StdlibClassIds.Any || classId != null && CfirExtendSemantics.isCType(classId)
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
