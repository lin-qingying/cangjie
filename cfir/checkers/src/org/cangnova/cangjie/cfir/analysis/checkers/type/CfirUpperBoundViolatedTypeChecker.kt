package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeContext
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
 * 检查类型使用处的泛型实参是否满足声明侧 upper bounds。
 *
 * 对齐 Kotlin FIR `FirUpperBoundViolatedTypeChecker`：类型解析阶段只构造类型，
 * 上界违反诊断在 resolved type ref checker 中基于已解析类型和原始 source ref 产生。
 */
object CfirUpperBoundViolatedTypeChecker : CfirResolvedTypeRefChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: CfirResolvedTypeRef) {
        if (typeRef is CfirErrorTypeRef) return
        checkUpperBoundViolated(
            type = typeRef.coneType,
            sourceTypeRef = typeRef.delegatedTypeRef,
            fallbackSource = typeRef.source,
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkUpperBoundViolated(
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

        val count = minOf(typeParameters.size, expandedType.typeArguments.size)
        val substitutor = createGenericUseSiteSubstitutor(
            typeParameters = typeParameters.take(count),
            resolvedArguments = expandedType.typeArguments.take(count).map { it.type },
            typeContext = context.session.typeContext,
        )
        val sourceArguments = sourceTypeRef.originalUserTypeRef()
            ?.qualifier
            ?.lastOrNull()
            ?.typeArguments
            .orEmpty()
        val canMapArgumentSources = sourceArguments.size == expandedType.typeArguments.size

        for (index in 0 until count) {
            val argumentType = expandedType.typeArguments[index].type
            val argumentSourceRef = sourceArguments.getOrNull(index).takeIf { canMapArgumentSources }
            val argumentSource = argumentSourceRef?.source?.firstCharacterDiagnosticSource()
                ?: sourceTypeRef?.source?.firstCharacterDiagnosticSource()
                ?: fallbackSource

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
                        reporter.reportOn(
                            source = argumentSource,
                            factory = CfirErrors.GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT,
                            a = argumentType,
                            b = upperBound,
                            c = type,
                        )
                    }
                }
            }

            checkUpperBoundViolated(
                type = argumentType,
                sourceTypeRef = argumentSourceRef,
                fallbackSource = argumentSourceRef?.source ?: argumentSource,
            )
        }
    }

    private fun createGenericUseSiteSubstitutor(
        typeParameters: List<CfirTypeParameterRef>,
        resolvedArguments: List<ConeCangJieType>,
        typeContext: ConeTypeContext,
    ) = createTypeSubstitutorByTypeConstructor(
        map = typeParameters.zip(resolvedArguments).associate { (typeParameter, argument) ->
            typeParameter.symbol.toLookupTag() as TypeConstructorMarker to argument
        },
        context = typeContext,
        approximateIntegerLiterals = false,
    )

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

    private fun CjSourceElement.firstCharacterDiagnosticSource(): CjSourceElement =
        fakeElement(
            CjFakeSourceElementKind.ErrorTypeRef,
            CjSourceElementOffsetStrategy.Custom.Initialized(
                startOffset = startOffset,
                endOffset = (startOffset + 1).coerceAtMost(endOffset),
            ),
        )
}
