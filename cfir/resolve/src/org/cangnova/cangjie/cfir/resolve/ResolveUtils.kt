package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.diagnostic.ConeHiddenCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostic.ConeVisibilityError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.diagnostic.ResolutionResultOverridesOtherToPreserveCompatibility
import org.cangnova.cangjie.cfir.diagnostic.VisibilityError
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedErrorReference
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirErrorFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirErrorNamedValueSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.type.model.safeSubstitute
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry


fun <T : CfirResolvable> BodyResolveComponents.typeFromCallee(access: T): ConeCangJieType {
    return typeFromCallee(access.calleeReference)
}

fun BodyResolveComponents.typeFromCallee(calleeReference: CfirReference): ConeCangJieType {
    return when (calleeReference) {
        is CfirErrorNamedReference -> {

            ConeErrorType(ConeUnreportedDuplicateDiagnostic(calleeReference.diagnostic))
        }

        is CfirNamedReferenceWithCandidate -> {
            val candidate = calleeReference.candidate
            if (candidate.symbol.cfir is CfirEnumConstructor) {
                return candidate.substitutedReturnType()
            }
            when (candidate.callInfo.callKind) {
                CallKind.NamedValueAccess -> typeFromNamedValueCandidate(candidate)
                else -> typeFromSymbol(candidate.symbol)
            }
        }

        is CfirResolvedAppliedCallableReference -> {
            if (calleeReference.resolvedSymbol.cfir is CfirEnumConstructor) {
                calleeReference.substitutedReturnType
                    ?: typeFromSymbol(calleeReference.resolvedSymbol)
            } else {
                typeFromSymbol(calleeReference.resolvedSymbol)
            }
        }

        is CfirResolvedNamedReference -> {
            typeFromSymbol(calleeReference.resolvedSymbol)
        }

        is CfirThisReference -> {
            val possibleImplicitReceivers = implicitValueStorage[null]
            when {
                possibleImplicitReceivers.size >= 2 -> ConeErrorType(
                    ConeSimpleDiagnostic("Ambiguous implicit this", DiagnosticKind.Other)
                )

                possibleImplicitReceivers.isEmpty() -> ConeErrorType(
                    ConeSimpleDiagnostic("Unresolved this", DiagnosticKind.Other)
                )

                else -> possibleImplicitReceivers.single().type
            }
        }

        is CfirSuperReference -> {
            calleeReference.superTypeRef.coneTypeOrNull
                ?: ConeErrorType(ConeSimpleDiagnostic("Unresolved super type", DiagnosticKind.Other))
        }

        else -> errorWithAttachment("Failed to extract type from: ${calleeReference::class.simpleName}") {
            withCfirEntry("reference", calleeReference)
        }
    }
}

private fun BodyResolveComponents.typeFromNamedValueCandidate(candidate: Candidate): ConeCangJieType {
    val declaration = candidate.symbol.cfir
    if (declaration !is CfirFunction) {
        if (declaration is CfirEnumConstructor) {
            return candidate.substitutedReturnType()
        }
        return typeFromSymbol(candidate.symbol)
    }

    val parameterTypes = declaration.valueParameters.map { parameter ->
        val resolvedType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            ?: return ConeErrorType(ConeSimpleDiagnostic("Unresolved function parameter type", DiagnosticKind.Other))
        runCatching { candidate.substitutor.substituteOrSelf(resolvedType) }.getOrDefault(resolvedType)
    }

    val returnType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType
        ?: return ConeErrorType(ConeSimpleDiagnostic("Unresolved function return type", DiagnosticKind.Other))
    val substitutedReturnType = runCatching { candidate.substitutor.substituteOrSelf(returnType) }.getOrDefault(returnType)

    return ConeFuncType(parameterTypes, substitutedReturnType)
}

private fun BodyResolveComponents.typeFromSymbol(symbol: CfirSymbol<*>): ConeCangJieType {
    return when (symbol) {
        is CfirCallableSymbol<*> -> {
            val returnTypeRef = returnTypeCalculator.tryCalculateReturnType(symbol.cfir)
            returnTypeRef.coneType
        }

        is CfirClassifierSymbol<*> -> {
            symbol.constructType()
        }

        else -> errorWithAttachment("Failed to extract type from symbol: ${symbol::class.java}") {
            withCfirEntry("declaration", symbol.cfir)
        }
    }
}

fun createConeDiagnosticForCandidateWithError(
    applicability: CandidateApplicability,
    candidate: Candidate,
): org.cangnova.cangjie.cfir.types.ConeDiagnostic {
    val visibilityError = candidate.diagnostics.firstOrNull { it is VisibilityError } as? VisibilityError
    if (visibilityError != null) {
        return ConeVisibilityError(visibilityError.symbol)
    }

    return when (applicability) {
        CandidateApplicability.HIDDEN -> ConeHiddenCandidateError(candidate)
        CandidateApplicability.VISIBILITY_ERROR -> {
            ConeVisibilityError(visibilityError?.symbol ?: candidate.symbol)
        }

        else -> ConeInapplicableCandidateError(applicability, candidate)
    }
}

internal fun Candidate.doesResolutionResultOverrideOtherToPreserveCompatibility(): Boolean =
    diagnostics.any { it === ResolutionResultOverridesOtherToPreserveCompatibility }

fun CfirTypeAliasSymbol.fullyExpandedClass(session: CfirSession): CfirClassLikeSymbol<*>? {
    if (!isBound) return null
    val expandedType = cfir.expandedTypeRef.coneTypeOrNull ?: return null
    val classId = when (expandedType) {
        is ConeClassLikeType -> expandedType.classId
        is ConeStructType -> expandedType.classId
        is ConeEnumType -> expandedType.classId
        is ConeTypeAliasType -> expandedType.expandedType?.let { nested ->
            when (nested) {
                is ConeClassLikeType -> nested.classId
                is ConeStructType -> nested.classId
                is ConeEnumType -> nested.classId
                else -> expandedType.classId
            }
        } ?: expandedType.classId

        else -> return null
    }
    return session.symbolProvider.getClassLikeSymbolByClassId(classId)
}

fun CfirNamedReferenceWithCandidate.toErrorReference(diagnostic: ConeDiagnostic): CfirNamedReference {
    val calleeReference = this
    val errorSource = calleeReference.source ?: calleeReference.candidate.callInfo.callSite.source
    return when (calleeReference.candidateSymbol) {
        is CfirErrorFunctionSymbol,
        is CfirErrorNamedValueSymbol -> buildErrorNamedReference {
            source = errorSource
            name = calleeReference.name
            this.diagnostic = diagnostic
        }

        else -> buildResolvedErrorReference {
            source = errorSource
            name = calleeReference.name
            resolvedSymbol = calleeReference.candidateSymbol
            this.diagnostic = diagnostic
        }
    }
}

fun ConeCangJieType.initialTypeOfCandidate(candidate: Candidate): ConeCangJieType {
    val system = candidate.system
    val resultingSubstitutor = system.buildCurrentSubstitutor()
    return resultingSubstitutor.safeSubstitute(system, candidate.substitutor.substituteOrSelf(this)).asCone()
}
