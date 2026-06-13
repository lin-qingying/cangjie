/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostic.*
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedErrorReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.type.model.safeSubstitute
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

fun BodyResolveComponents.initialTypeOfCandidate(candidate: Candidate): ConeCangJieType {
    val type = typeFromSymbol(candidate.symbol)
    return type.initialTypeOfCandidate(candidate)
}
val CfirTypeParameterSymbol.defaultType: ConeTypeParameterType
    get() = ConeTypeParameterTypeImpl(toLookupTag())

fun <T : CfirResolvable> BodyResolveComponents.typeFromCallee(access: T): ConeCangJieType {
    if (access is CfirQualifiedAccessExpression && access.typeArguments.isNotEmpty()) {
        val classifierSymbol = (access.calleeReference as? CfirResolvedNamedReference)
            ?.resolvedSymbol as? CfirClassifierSymbol<*>
        if (classifierSymbol != null) {
            val typeArguments = access.typeArguments.map { typeArgument ->
                typeArgument.coneTypeOrNull ?: return ConeErrorType(
                    ConeSimpleDiagnostic("Unresolved qualifier type argument", DiagnosticKind.Other)
                )
            }
            return classifierSymbol.constructTypeForQualifiedAccess(typeArguments)
        }
    }
    return typeFromCallee(access.calleeReference)
}

fun BodyResolveComponents.typeFromCallee(calleeReference: CfirReference): ConeCangJieType {
    return when (calleeReference) {
        is CfirNamedReferenceWithCandidate -> {
            val candidate = calleeReference.candidate
            // 函数类型变量以调用形式使用时，对齐 synthetic invoke：表达式类型是函数类型的返回值。
            if (candidate.callInfo.callKind == CallKind.Function && candidate.symbol.cfir is CfirVariable) {
                return candidate.substitutedReturnType()
            }
            if (candidate.symbol.cfir is CfirEnumConstructor) {
                return candidate.substitutedReturnType()
            }
            when (candidate.callInfo.callKind) {
                CallKind.NamedValueAccess -> typeFromNamedValueCandidate(candidate)
                else -> typeFromSymbol(candidate.symbol)
            }
        }

        is CfirErrorNamedReference -> {

            ConeErrorType(ConeUnreportedDuplicateDiagnostic(calleeReference.diagnostic))
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

    return functionTypeForFunctionValueCandidate(candidate, declaration)
}

/**
 * 构造“函数名作为值”时的函数类型。
 *
 * 仓颉函数是一等值，`let f: (Int64) -> Unit = g` 这类引用不能使用
 * `g` 的返回值类型，而必须使用完整函数签名参与后续期望类型与重载解析。
 */
fun BodyResolveComponents.functionTypeForFunctionValueCandidate(
    candidate: Candidate,
    declaration: CfirFunction = candidate.symbol.cfir as CfirFunction,
): ConeCangJieType {
    val parameterTypes = declaration.valueParameters.map { parameter ->
        val resolvedType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            ?: return ConeErrorType(ConeSimpleDiagnostic("Unresolved function parameter type", DiagnosticKind.Other))
        candidate.substitutor.substituteOrSelf(resolvedType)
    }

    returnTypeCalculator.tryCalculateReturnType(declaration)
    val substitutedReturnType = candidate.substitutedReturnType().approximateThisTypeForDeclaration()

    return ConeFunctionType(parameterTypes, substitutedReturnType)
}

private fun BodyResolveComponents.typeFromSymbol(symbol: CfirBasedSymbol<*>): ConeCangJieType {
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

private fun CfirClassifierSymbol<*>.constructTypeForQualifiedAccess(
    typeArguments: List<ConeTypeProjection>,
): ConeCangJieType = when (this) {
    is CfirTypeAliasSymbol -> ConeTypeAliasType(classId, typeArguments = typeArguments)
    else -> constructType(typeArguments)
}

fun createConeDiagnosticForCandidateWithError(
    applicability: CandidateApplicability,
    candidate: Candidate,
): org.cangnova.cangjie.cfir.types.ConeDiagnostic {
    val visibilityError = candidate.diagnostics.firstOrNull { it is VisibilityError } as? VisibilityError
    if (visibilityError != null) {
        return ConeVisibilityError(visibilityError.symbol)
    }
    if (candidate.system.hasContradiction) {
        return ConeConstraintSystemHasContradiction(candidate)
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
