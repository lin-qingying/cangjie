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
import org.cangnova.cangjie.cfir.types.ConeTypeContext
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
internal fun checkUpperBoundViolated(typeRef: CfirResolvedTypeRef) {
    if (typeRef is CfirErrorTypeRef) return
    checkUpperBoundViolated(
        type = typeRef.coneType,
        sourceTypeRef = typeRef.delegatedTypeRef,
        fallbackSource = typeRef.source,
    )
}

context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkUpperBoundViolated(
    type: ConeCangJieType,
    sourceTypeRef: CfirTypeRef?,
    fallbackSource: CjSourceElement?,
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
        },
        sourceTypeRefs = sourceArguments.takeIf { canMapArgumentSources },
        fallbackSource = sourceTypeRef?.source ?: fallbackSource,
        substitutor = substitutor,
        diagnosticGenericType = expandedType,
    )
}

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

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkUpperBoundViolated(
    typeParameters: List<CfirTypeParameterRef>,
    argumentTypes: List<ConeCangJieType?>,
    argumentSources: List<CjSourceElement?>,
    sourceTypeRefs: List<CfirTypeRef>?,
    fallbackSource: CjSourceElement?,
    substitutor: ConeSubstitutor,
    diagnosticGenericType: ConeCangJieType?,
) {
    val count = minOf(typeParameters.size, argumentTypes.size)
    for (index in 0 until count) {
        val argumentType = argumentTypes[index] ?: continue
        val sourceTypeRef = sourceTypeRefs?.getOrNull(index)
        val argumentSource = argumentSources.getOrNull(index) ?: fallbackSource

        if (argumentType !is ConeErrorType && !argumentType.isGenericTypeWithInvalidUpperBound()) {
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
        )
    }
}

private fun ConeCangJieType.isGenericTypeWithInvalidUpperBound(): Boolean {
    val typeParameterType = this as? ConeTypeParameterType ?: return false
    return typeParameterType.lookupTag.typeParameterSymbol.resolvedBounds.any { it.coneType is ConeErrorType }
}

private fun CfirTypeRef?.originalUserTypeRef(): CfirUserTypeRef? =
    when (this) {
        is CfirUserTypeRef -> this
        is CfirResolvedTypeRef -> delegatedTypeRef.originalUserTypeRef()
        else -> null
    }

internal fun CjSourceElement.firstCharacterDiagnosticSource(): CjSourceElement =
    fakeElement(
        CjFakeSourceElementKind.ErrorTypeRef,
        CjSourceElementOffsetStrategy.Custom.Initialized(
            startOffset = startOffset,
            endOffset = (startOffset + 1).coerceAtMost(endOffset),
        ),
    )
