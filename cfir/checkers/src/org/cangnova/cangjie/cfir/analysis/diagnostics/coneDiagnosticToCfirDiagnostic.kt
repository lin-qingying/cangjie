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

package org.cangnova.cangjie.cfir.analysis.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostic.*
import org.cangnova.cangjie.cfir.diagnostics.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.inference.AnonymousFunctionBasedMultiLambdaBuilderInferenceRestriction
import org.cangnova.cangjie.cfir.resolve.inference.model.*
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.semantics.isSuccess
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getAssignmentByLHS
import org.cangnova.cangjie.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeKind
import org.cangnova.cangjie.source.*
import org.cangnova.cangjie.type.model.TypeParameterMarker

fun ConeDiagnostic.toCfirDiagnostics(
    session: CfirSession,
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    valueParameter: CfirValueParameter? = null,
): List<CjDiagnostic> {
    if (this is ConeUnreportedDuplicateDiagnostic) return emptyList()
    return when (this) {
        is ConeConstraintSystemHasContradiction -> mapSystemHasContradictionError(session, source, callOrAssignmentSource)
        is ConeInapplicableCandidateError -> mapInapplicableCandidateError(session, source, callOrAssignmentSource)
        is ConeAmbiguityError -> mapConeAmbiguityError(source, callOrAssignmentSource, session)
        is ConeUnresolvedNameError -> mapConeUnresolvedNameError(source, callOrAssignmentSource, session)
        is ConeVisibilityError -> listOfNotNull(mapConeVisibilityError(source, callOrAssignmentSource, session))
        else -> listOfNotNull(mapOtherDiagnostic(source, valueParameter, callOrAssignmentSource, session))
    }
}

private fun ConstraintSystemError.mapConstraintSystemError(
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
    session: CfirSession,
    candidate: AbstractCallCandidate<*>,
): CjDiagnostic? {
    return when (this) {
        is ConstraintMismatch -> {
            if (candidate.symbol.let { it as? CfirCallableSymbol<*> }?.cfir?.typeParameters?.isNotEmpty() == true &&
                !candidate.hasExplicitTypeArgumentsInCall()
            ) {
                ConeConstraintSystemHasContradiction(candidate)
                    .genericInferenceErrorDiagnostic(source, qualifiedAccessSource, session)
                    ?.let { return it }
            }

            val position = position.from
            val argumentAndReportSource: Pair<org.cangnova.cangjie.cfir.CfirElement?, CjSourceElement?> = when (position) {
                is ConeArgumentConstraintPosition -> position.argument to null
                is ConeLambdaArgumentConstraintPosition -> position.argument to position.anonymousFunctionReturnExpression?.source
                is ConeReceiverConstraintPosition -> {
                    val reportOn = position.argument.source?.takeIf { it.kind == CjRealSourceElementKind }
                    position.argument to reportOn
                }

                else -> null to null
            }
            val argument = argumentAndReportSource.first
            val reportOn = argumentAndReportSource.second

            argument?.let {
                if (!candidate.hasExplicitTypeArgumentsInCall()) {
                    (it as? org.cangnova.cangjie.cfir.expressions.CfirExpression)
                        ?.genericInferenceArgumentMismatchDiagnostic(session)
                        ?.let { inferenceDiagnostic ->
                            return inferenceDiagnostic
                        }
                }
                return argumentTypeMismatch(
                    source = reportOn ?: it.source ?: source,
                    expectedType = upperConeType.substituteTypeVariableTypes(candidate, session),
                    actualType = lowerConeType.substituteTypeVariableTypes(candidate, session),
                    isMismatchDueToNullability = false,
                    anonymousFunction = (position as? ConeLambdaArgumentConstraintPosition)?.argument,
                    session = session,
                )
            }

            when (position) {
                is ConeExpectedTypeConstraintPosition ->
                    typeMismatchDiagnostic(
                        source = when (candidate.callInfo.callSite) {
                            is CfirNamedAccessExpression -> qualifiedAccessSource
                                ?: source
                                ?: candidate.callInfo.callSite.source

                            else -> qualifiedAccessSource ?: source
                        },
                        callOrAssignmentSource = qualifiedAccessSource,
                        expectedType = upperConeType.substituteTypeVariableTypes(candidate, session),
                        actualType = lowerConeType.substituteTypeVariableTypes(candidate, session),
                        isMismatchDueToNullability = false,
                        session = session,
                    )

                else -> null
            }
        }

        is NotEnoughInformationForTypeParameter<*> ->
            if (
                candidate.isImplicitBuiltinArrayConstructorCall() ||
                candidate.isImplicitGenericCallWithTypeParameters() ||
                candidate.hasGenericCallNotEnoughTypeInformation() ||
                candidate.looksLikeImplicitGenericCallInferenceFailure(source, qualifiedAccessSource)
            ) {
                ConeConstraintSystemHasContradiction(candidate)
                    .unableToInferGenericFunctionDiagnostic(source, qualifiedAccessSource, session)
            } else {
                typeVariable.asDeclaredTypeParameterSymbolOrNull()?.let {
                    ConeConstraintSystemHasContradiction(candidate)
                        .unableToInferGenericFunctionDiagnostic(source, qualifiedAccessSource, session)
                }
            }

        is InferredEmptyIntersection -> {
            inferredIntoEmptyIntersection(
                source = candidate.sourceOfCallToSymbolWith(typeVariable) ?: source ?: qualifiedAccessSource,
                typeVariable = typeVariable,
                incompatibleTypes = incompatibleTypes.map { it.asCone() },
                causingTypes = causingTypes.map { it.asCone() },
                kind = kind,
                isError = this is InferredEmptyIntersectionError,
                session = session,
            )
        }

        is OnlyInputTypesDiagnostic ->
            typeVariable.asDeclaredTypeParameterSymbolOrNull()?.let {
                CfirErrors.TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR.on(source ?: qualifiedAccessSource ?: return null, it, session)
            }

        is AnonymousFunctionBasedMultiLambdaBuilderInferenceRestriction -> {
            val typeParameterSymbol = typeParameter.asDeclaredTypeParameterSymbolOrNull() ?: return null
            val containingDeclarationName = typeParameterSymbol.containingDeclarationSymbol.memberDeclarationNameOrNull()
                ?: error("containingDeclarationSymbol must have been a member declaration")
            CfirErrors.BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION.on(
                anonymous.source ?: source ?: qualifiedAccessSource ?: return null,
                typeParameterSymbol.name,
                containingDeclarationName,
                session,
            )
        }

        is MultiLambdaBuilderInferenceRestriction<*> -> error("Unexpected bare MultiLambdaBuilderInferenceRestriction")

        else -> null
    }
}

private fun ConeConstraintSystemHasContradiction.mapSystemHasContradictionError(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): List<CjDiagnostic> {
    candidate.cangjieVariadicRegularCallDiagnostics
        .mapCangjieVariadicRegularCallDiagnostics(session, source, qualifiedAccessSource)
        .takeIf { it.isNotEmpty() }
        ?.let { return it }

    candidate.diagnostics
        .filter { it.isArgumentMappingDiagnostic }
        .mapCangjieVariadicRegularCallDiagnostics(session, source, qualifiedAccessSource)
        .takeIf { it.isNotEmpty() }
        ?.let { return it }

    val errors = candidate.errors
    candidate.diagnostics
        .filterIsInstance<TooManyArguments>()
        .mapNotNull { diagnostic ->
            CfirErrors.TOO_MANY_ARGUMENTS.on(
                diagnostic.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
                diagnostic.targetName,
                session,
            )
        }
        .takeIf { it.isNotEmpty() }
        ?.let { return it }
    if (candidate.callInfo.arguments.any { it.containsErrorDiagnosticInArgument() }) return emptyList()

    if (isBareStaticGenericQualifierInferenceError(session)) {
        return listOfNotNull(
            CfirErrors.UNABLE_TO_INFER_GENERIC_FUNC.on(
                qualifiedAccessSource ?: source ?: return emptyList(),
                session,
            )
        )
    }
    if (candidate.hasGenericCallNotEnoughTypeInformation()) {
        return listOfNotNull(unableToInferGenericFunctionDiagnostic(source, qualifiedAccessSource, session))
    }
    explicitTypeArgumentConstraintMismatchDiagnostic(source, qualifiedAccessSource, session)
        ?.let { return listOf(it) }
    if (hasGenericInferenceConstraintMismatch()) {
        return listOfNotNull(genericInferenceErrorDiagnostic(source, qualifiedAccessSource, session))
    }

    val hasNotEnoughInformationError = errors.any { it is NotEnoughInformationForTypeParameter<*> }
    return errors.coalesceArgumentConstraintMismatches().coalesceExpectedTypeConstraintMismatches().mapNotNull { error ->
        if (hasNotEnoughInformationError &&
            error is ConstraintMismatch &&
            error.position.from is ConeExpectedTypeConstraintPosition
        ) {
            // When generic arguments are absent and type inference itself failed,
            // expected-type mismatches are usually secondary noise.
            return@mapNotNull null
        }
        error.mapConstraintSystemError(
            source,
            qualifiedAccessSource,
            session,
            candidate,
        )
    }.ifEmpty {
        if (errors.any { error ->
                when (error) {
                    is ConstrainingTypeIsError -> true
                    is NotEnoughInformationForTypeParameter<*> ->
                        error.typeVariable is ConeTypeParameterBasedTypeVariable ||
                            (error.resolvedAtom as? CfirAnonymousFunction)?.containsErrorType() == true

                    else -> false
                }
            }
        ) {
            return emptyList()
        }

        listOfNotNull(
            errors.firstNotNullOfOrNull { error ->
                when (error) {
                    is ConstraintMismatch -> {
                        if (error.position.from is FixVariableConstraintPosition<*>) {
                            val morePreciseDiagnosticExists = errors.any { other ->
                                other is ConstraintMismatch && other.position.from !is FixVariableConstraintPosition<*>
                            }
                            if (morePreciseDiagnosticExists) return@firstNotNullOfOrNull null
                        }

                        typeMismatchDiagnostic(
                            source = qualifiedAccessSource ?: source,
                            callOrAssignmentSource = qualifiedAccessSource,
                            expectedType = error.upperConeType.substituteTypeVariableTypes(candidate, session),
                            actualType = error.lowerConeType.substituteTypeVariableTypes(candidate, session),
                            isMismatchDueToNullability = false,
                            session = session,
                        )
                    }

                    else -> CfirErrors.NEW_INFERENCE_ERROR.on(
                        qualifiedAccessSource ?: source ?: return@firstNotNullOfOrNull null,
                        "Inference error: ${error::class.simpleName}",
                        session,
                    )
                }
            }
        )
    }
}

private fun ConeConstraintSystemHasContradiction.isBareStaticGenericQualifierInferenceError(
    session: CfirSession,
): Boolean {
    val callable = candidate.symbol.cfir as? CfirCallableDeclaration ?: return false
    if (!callable.status.isStatic) return false
    if (candidate.errors.none { it is NotEnoughInformationForTypeParameter<*> }) return false

    val receiver = candidate.callInfo.explicitReceiver as? CfirQualifiedAccessExpression ?: return false
    if (receiver.typeArguments.isNotEmpty()) return false

    val ownerSymbol = receiver.resolvedQualifierClassifier(session) ?: return false
    return ownerSymbol.cfir.typeParameters.isNotEmpty()
}

private fun ConeInapplicableCandidateError.mapInapplicableCandidateError(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): List<CjDiagnostic> {
    candidate.cangjieVariadicRegularCallDiagnostics
        .mapCangjieVariadicRegularCallDiagnostics(session, source, qualifiedAccessSource)
        .takeIf { it.isNotEmpty() }
        ?.let { return it }

    val contradictionDiagnostic = ConeConstraintSystemHasContradiction(candidate)
    contradictionDiagnostic.multiLambdaBuilderInferenceDiagnostics(session, source, qualifiedAccessSource)
        .takeIf { it.isNotEmpty() }
        ?.let { return it }

    val noMatchingInvokeDiagnostic = mapNoMatchingInvokeOperatorDiagnostic(session, source, qualifiedAccessSource)
    val genericDiagnostic = (qualifiedAccessSource ?: source)?.let { diagnosticSource ->
        when (candidateSymbol.cfir) {
            is org.cangnova.cangjie.cfir.declarations.CfirConstructor,
            is CfirEnumConstructor,
            -> CfirErrors.NO_CONSTRUCTOR.on(diagnosticSource, session)

            else -> CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, candidateSymbol.debugName, null, session)
        }
    }

    var suppressedRangeArgumentMismatch = false
    var suppressedErrorTypeInArguments = false
    val hasErrorTypeInArguments = candidate.diagnostics.any { it == ErrorTypeInArguments }
    val diagnostics = candidate.diagnostics.filter { !it.isSuccess }.coalesceArgumentTypeMismatches().mapNotNull { rootCause ->
        when (rootCause) {
            ErrorTypeInArguments -> {
                suppressedErrorTypeInArguments = true
                null
            }

            is ArgumentPassedTwice -> CfirErrors.ARGUMENT_PASSED_TWICE.on(
                rootCause.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
                session,
            )

            is ArgumentTypeMismatch -> {
                candidate.multiLambdaBuilderInferenceDiagnosticFor(rootCause.argument, source, qualifiedAccessSource, session)
                    ?.let { return@mapNotNull it }
                if (!candidate.hasExplicitTypeArgumentsInCall()) {
                    rootCause.argument.genericInferenceArgumentMismatchDiagnostic(session)
                        ?.let { return@mapNotNull it }
                    rootCause.argument.genericInferenceCallTypeMismatchDiagnostic(session)
                        ?.let { return@mapNotNull it }
                }
                if (!candidate.usedOuterCs &&
                    rootCause.systemHadContradiction &&
                    !candidate.hasExplicitTypeArgumentsInCall()
                ) {
                    return@mapNotNull null
                }
                if (candidate.hasGenericCallNotEnoughTypeInformation() && rootCause.argument is CfirAnonymousFunctionExpression) {
                    return@mapNotNull null
                }

                val expectedType = rootCause.expectedType.substituteTypeVariableTypes(candidate, session)
                val actualType =
                    if (rootCause.argument is CfirAnonymousFunctionExpression && rootCause.argument.coneTypeOrNull?.isError == false) {
                        rootCause.argument.coneTypeOrNull!!
                    } else {
                        rootCause.actualType.substituteTypeVariableTypes(candidate, session)
                    }

                argumentTypeMismatch(
                    source = rootCause.argument.source ?: source,
                    expectedType = expectedType,
                    actualType = actualType,
                    isMismatchDueToNullability = rootCause.isMismatchDueToNullability,
                    anonymousFunction = rootCause.anonymousFunctionIfReturnExpression,
                    session = session,
                ).also { diagnostic ->
                    if (diagnostic == null &&
                        expectedType.rangeElementTypeOrNull() != null &&
                        actualType.rangeElementTypeOrNull() != null
                    ) {
                        suppressedRangeArgumentMismatch = true
                    }
                }
            }

            is AmbiguousArgumentType -> {
                val diagnosticSource = rootCause.callSite.source
                    ?: qualifiedAccessSource
                    ?: source
                    ?: candidate.callInfo.callSite.source
                    ?: return@mapNotNull null
                CfirErrors.AMBIGUOUS_ARG_TYPE.on(
                    diagnosticSource.firstCharacterDiagnosticSource(),
                    candidate.callInfo.name,
                    session,
                )
            }

            is InferenceConstraintError -> {
                if (hasErrorTypeInArguments) return@mapNotNull null
                if (candidate.diagnostics.any { it is TooManyArguments }) return@mapNotNull null
                if (candidate.callInfo.arguments.any { it.containsErrorDiagnosticInArgument() }) return@mapNotNull null
                if (candidate.hasExplicitTypeArgumentsInCall() &&
                    candidate.diagnostics.any { it is ArgumentTypeMismatch }
                ) {
                    return@mapNotNull null
                }
                if (candidate.symbol.let { it as? CfirCallableSymbol<*> }?.cfir?.typeParameters?.isNotEmpty() == true &&
                    !candidate.hasExplicitTypeArgumentsInCall()
                ) {
                    ConeConstraintSystemHasContradiction(candidate)
                        .genericInferenceErrorDiagnostic(source, qualifiedAccessSource, session)
                } else {
                    CfirErrors.NEW_INFERENCE_ERROR.on(
                        qualifiedAccessSource ?: source ?: candidate.callInfo.callSite.source ?: return@mapNotNull null,
                        rootCause.message,
                        session,
                    )
                }
            }

            is MixingNamedAndPositionalArguments -> CfirErrors.MIXING_NAMED_AND_POSITIONAL_ARGUMENTS.on(
                rootCause.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
                session,
            )

            is NamedArgumentsNotAllowed -> CfirErrors.NAMED_ARGUMENTS_NOT_ALLOWED.on(
                rootCause.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
                rootCause.targetDescription,
                session,
            )

            is NamedParameterNotFound -> CfirErrors.NAMED_PARAMETER_NOT_FOUND.on(
                rootCause.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
                rootCause.name,
                session,
            )

            is NeedNamedArgument -> CfirErrors.NEED_NAMED_ARGUMENT.on(
                rootCause.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
                rootCause.parameter.name,
                session,
            )

            is NoValueForParameter -> CfirErrors.NO_VALUE_FOR_PARAMETER.on(
                qualifiedAccessSource ?: source ?: return@mapNotNull null,
                rootCause.valueParameter.name,
                session,
            )

            is TooManyArguments -> CfirErrors.TOO_MANY_ARGUMENTS.on(
                rootCause.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
                rootCause.targetName,
                session,
            )

            else -> genericDiagnostic
        }
    }

    if (diagnostics.isNotEmpty()) return listOfNotNull(noMatchingInvokeDiagnostic) + diagnostics
    if (suppressedRangeArgumentMismatch) return listOfNotNull(noMatchingInvokeDiagnostic)
    if (suppressedErrorTypeInArguments) return listOfNotNull(noMatchingInvokeDiagnostic)

    noMatchingInvokeDiagnostic?.let { return listOf(it) }

    genericInferenceInapplicableDiagnostic(session, source, qualifiedAccessSource)
        ?.let { return listOf(it) }

    val diagnosticSource = qualifiedAccessSource ?: source ?: return emptyList()
    return listOfNotNull(CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, candidateSymbol.debugName, null, session))
}

private fun List<ResolutionDiagnostic>.mapCangjieVariadicRegularCallDiagnostics(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): List<CjDiagnostic> = coalesceArgumentMappingDiagnostics().mapNotNull { diagnostic ->
    when (diagnostic) {
        is ArgumentPassedTwice -> CfirErrors.ARGUMENT_PASSED_TWICE.on(
            diagnostic.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
            session,
        )

        is MixingNamedAndPositionalArguments -> CfirErrors.MIXING_NAMED_AND_POSITIONAL_ARGUMENTS.on(
            diagnostic.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
            session,
        )

        is NamedArgumentsNotAllowed -> CfirErrors.NAMED_ARGUMENTS_NOT_ALLOWED.on(
            diagnostic.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
            diagnostic.targetDescription,
            session,
        )

        is NamedParameterNotFound -> CfirErrors.NAMED_PARAMETER_NOT_FOUND.on(
            diagnostic.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
            diagnostic.name,
            session,
        )

        is NeedNamedArgument -> CfirErrors.NEED_NAMED_ARGUMENT.on(
            diagnostic.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
            diagnostic.parameter.name,
            session,
        )

        is NoValueForParameter -> CfirErrors.NO_VALUE_FOR_PARAMETER.on(
            qualifiedAccessSource ?: source ?: return@mapNotNull null,
            diagnostic.valueParameter.name,
            session,
        )

        is TooManyArguments -> CfirErrors.TOO_MANY_ARGUMENTS.on(
            diagnostic.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
            diagnostic.targetName,
            session,
        )

        else -> null
    }
}

/**
 * 官方参数个数错误是调用级诊断；本项目暂以 `NO_VALUE_FOR_PARAMETER`
 * 表达缺参语义时，同一候选上多个缺失形参应合并成一个用户可见诊断。
 */
private fun List<ResolutionDiagnostic>.coalesceArgumentMappingDiagnostics(): List<ResolutionDiagnostic> {
    var reportedNoValueForParameter = false
    return filter { diagnostic ->
        if (diagnostic !is NoValueForParameter) return@filter true
        if (reportedNoValueForParameter) return@filter false
        reportedNoValueForParameter = true
        true
    }
}

private fun ConeConstraintSystemHasContradiction.multiLambdaBuilderInferenceDiagnostics(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): List<CjDiagnostic> {
    return candidate.errors
        .filterIsInstance<ConstraintSystemError>()
        .filter {
            it is AnonymousFunctionBasedMultiLambdaBuilderInferenceRestriction ||
                it is MultiLambdaBuilderInferenceRestriction<*>
        }
        .mapNotNull { error ->
            error.mapConstraintSystemError(
                source = source,
                qualifiedAccessSource = qualifiedAccessSource,
                session = session,
                candidate = candidate,
            )
        }
}

private fun AbstractCallCandidate<*>.multiLambdaBuilderInferenceDiagnosticFor(
    argument: org.cangnova.cangjie.cfir.CfirElement,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val anonymousFunction = (argument as? CfirAnonymousFunctionExpression)?.anonymousFunction ?: return null
    val restriction = errors
        .filterIsInstance<AnonymousFunctionBasedMultiLambdaBuilderInferenceRestriction>()
        .firstOrNull { it.anonymous == anonymousFunction }
        ?: return null
    return restriction.mapConstraintSystemError(
        source = source,
        qualifiedAccessSource = qualifiedAccessSource,
        session = session,
        candidate = this,
    )
}

private fun ConeInapplicableCandidateError.genericInferenceInapplicableDiagnostic(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): CjDiagnostic? {
    val contradiction = ConeConstraintSystemHasContradiction(candidate)
    val hasInferenceConstraintDiagnostic = !candidate.hasExplicitTypeArgumentsInCall() &&
        candidate.diagnostics.any { it is InferenceConstraintError }
    if (!candidate.hasGenericInferenceArgumentMismatch() &&
        !candidate.hasGenericCallNotEnoughTypeInformation() &&
        !contradiction.hasGenericInferenceConstraintMismatch() &&
        !hasInferenceConstraintDiagnostic
    ) {
        return null
    }
    return contradiction.genericInferenceErrorDiagnostic(source, qualifiedAccessSource, session)
}

private fun ConeInapplicableCandidateError.mapNoMatchingInvokeOperatorDiagnostic(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): CjDiagnostic? {
    if (candidateSymbol.memberDeclarationNameOrNull() != OperatorNameConventions.INVOKE) return null
    val receiverType = candidate.callInfo.explicitReceiver?.coneTypeOrNull ?: return null
    if (receiverType is ConeErrorType) return null
    val diagnosticSource = qualifiedAccessSource ?: source ?: return null
    return CfirErrors.NO_MATCHING_OPERATOR_INVOKE.on(
        diagnosticSource,
        diagnosticSource.text?.toString().orEmpty(),
        receiverType,
        session,
    )
}

private fun argumentTypeMismatch(
    source: CjSourceElement?,
    expectedType: ConeCangJieType,
    actualType: ConeCangJieType,
    isMismatchDueToNullability: Boolean,
    anonymousFunction: CfirFunction?,
    session: CfirSession,
): CjDiagnostic? {
    if (source == null) return null
    if (expectedType.containsErrorType() || actualType.containsErrorType()) return null
    if (expectedType.rangeElementTypeOrNull() != null &&
        actualType.rangeElementTypeOrNull() != null
    ) {
        return null
    }
    specificTypeMismatchDiagnostic(
        source = source,
        expectedType = expectedType,
        actualType = actualType,
        session = session,
    )?.let { return it }

    if (expectedType.isFunctionTypeLike() && actualType.isFunctionTypeLike()) {
        return CfirErrors.TYPE_MISMATCH.on(
            source,
            expectedType,
            actualType,
            isMismatchDueToNullability,
            session,
        )
    }

    if (anonymousFunction != null) {
        val lambdaSource = anonymousFunction.source ?: source
        return CfirErrors.TYPE_MISMATCH.on(
            lambdaSource,
            expectedType,
            actualType,
            isMismatchDueToNullability,
            session,
        )
    }

    return CfirErrors.ARGUMENT_TYPE_MISMATCH.on(
        source,
        expectedType,
        actualType,
        isMismatchDueToNullability,
        session,
    )
}

private fun ConeCangJieType.rangeElementTypeOrNull(): ConeCangJieType? = when (this) {
    is ConeClassLikeType -> if (classId == StdlibClassIds.Range) typeArguments.singleOrNull()?.type else null
    is ConeStructType -> if (classId == StdlibClassIds.Range) typeArguments.singleOrNull()?.type else null
    is ConeTypeAliasType -> expandedType?.rangeElementTypeOrNull()
    else -> null
}

/**
 * 函数值作为实参时，参数/返回值结构子类型检查失败属于函数类型整体不匹配。
 * 官方 Cangjie 在 `IsFuncSubtype` 失败后仍走 `sema_mismatched_types`，
 * 因此这里不降成普通实参专用诊断。
 */
private fun ConeCangJieType.isFunctionTypeLike(): Boolean = when (this) {
    is ConeFunctionType -> true
    is ConeTypeAliasType -> expandedType?.isFunctionTypeLike() == true
    else -> false
}

private fun ConeAmbiguityError.mapConeAmbiguityError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): List<CjDiagnostic> {
    @OptIn(ApplicabilityDetail::class)
    if (!applicability.isSuccess) {
        val candidateDiagnostics = candidatesWithErrors.values.map { coneDiagnostic ->
            coneDiagnostic?.toCfirDiagnostics(
                session = session,
                source = source,
                callOrAssignmentSource = callOrAssignmentSource,
                valueParameter = null,
            ).orEmpty()
        }

        val diagnosticsByKey = candidateDiagnostics
            .map { diagnostics -> diagnostics.distinctBy { it.diagnosticIdentityKey() } }
            .filter { it.isNotEmpty() }
        if (diagnosticsByKey.isNotEmpty()) {
            val sharedDiagnosticKeys = diagnosticsByKey
                .map { diagnostics -> diagnostics.map { it.diagnosticIdentityKey() }.toSet() }
                .reduce { acc, keys -> acc.intersect(keys) }

            val sharedDiagnostics = diagnosticsByKey
                .first()
                .filter { it.diagnosticIdentityKey() in sharedDiagnosticKeys }
            if (sharedDiagnostics.isNotEmpty()) {
                val isInvokeOperatorAmbiguity = candidateSymbols.any { symbol ->
                    symbol.memberDeclarationNameOrNull() == OperatorNameConventions.INVOKE
                }
                if (isInvokeOperatorAmbiguity) {
                    val sharedAnchorKeys = diagnosticsByKey
                        .map { diagnostics -> diagnostics.map { it.diagnosticAnchorKey() }.toSet() }
                        .reduce { acc, keys -> acc.intersect(keys) }
                    val sharedByAnchorDiagnostics = diagnosticsByKey
                        .first()
                        .filter { diagnostic ->
                            diagnostic.diagnosticAnchorKey() in sharedAnchorKeys &&
                                diagnostic.diagnosticIdentityKey() !in sharedDiagnosticKeys
                        }

                    return (sharedDiagnostics + sharedByAnchorDiagnostics)
                        .distinctBy { it.diagnosticAnchorKey() }
                }

                return sharedDiagnostics
            }

            return diagnosticsByKey.first()
        }
    }

    if (candidateSymbols.all { symbol ->
            symbol.cfir is org.cangnova.cangjie.cfir.declarations.CfirConstructor || symbol.cfir is CfirEnumConstructor
        }
    ) {
        val diagnosticSource = callOrAssignmentSource ?: source ?: return emptyList()
        return listOfNotNull(CfirErrors.AMBIGUOUS_CONSTRUCTOR_CALL.on(diagnosticSource, name, session))
    }

    val diagnosticSource = callOrAssignmentSource ?: source ?: return emptyList()
    val psi = diagnosticSource.psi
    val isCallLikeContext = psi is CjCallExpression || PsiTreeUtil.getParentOfType(psi, CjCallExpression::class.java, false) != null

    // 检查是否为基本类型扩展歧义
    if (isCallLikeContext) {
        val extendOriginNames = candidateSymbols
            .mapNotNull { symbol ->
                val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return@mapNotNull null
                val containingClassId = session.cfirProvider.getContainingClass(callableSymbol)?.classId
                containingClassId?.shortClassName
            }
        if (extendOriginNames.size >= 2) {
            // 如果候选来自不同的 extend 目标类型，报告 AMBIGUOUS_MATCH_PRIMITIVE_EXTEND
            val distinctOrigins = extendOriginNames.distinct()
            if (distinctOrigins.size >= 2) {
                return listOfNotNull(
                    CfirErrors.AMBIGUOUS_MATCH_PRIMITIVE_EXTEND.on(
                        diagnosticSource,
                        name,
                        distinctOrigins.map { it },
                        session,
                    )
                )
            }
        }
    }

    val factory = if (isCallLike || isCallLikeContext) CfirErrors.AMBIGUOUS_FUNCTION_CALL else CfirErrors.AMBIGUOUS_USE
    return listOfNotNull(factory.on(diagnosticSource, name, session))
}

private data class DiagnosticIdentityKey(
    val factoryName: String,
    val message: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class DiagnosticAnchorKey(
    val factoryName: String,
    val startOffset: Int,
    val endOffset: Int,
)

private fun CjDiagnostic.diagnosticIdentityKey(): DiagnosticIdentityKey =
    DiagnosticIdentityKey(
        factoryName = factoryName,
        message = renderMessage(),
        startOffset = firstRange.startOffset,
        endOffset = firstRange.endOffset,
    )

private fun CjDiagnostic.diagnosticAnchorKey(): DiagnosticAnchorKey =
    DiagnosticAnchorKey(
        factoryName = factoryName,
        startOffset = firstRange.startOffset,
        endOffset = firstRange.endOffset,
    )

private fun ConeUnresolvedNameError.mapConeUnresolvedNameError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): List<CjDiagnostic> {
    mapExtendSuperDiagnostic(source, callOrAssignmentSource, session)?.let { diagnostic ->
        return listOf(diagnostic)
    }
    mapGenericUpperBoundAccessDiagnostic(source, callOrAssignmentSource, session)?.let { diagnostic ->
        return listOf(diagnostic)
    }
    mapSubscriptOperatorDiagnostic(source, callOrAssignmentSource, session)?.let { diagnostic ->
        return listOf(diagnostic)
    }

    // 一元运算符解析失败：有 operator 和 receiverType 但无参数
    mapInvalidUnaryExprDiagnostic(source, callOrAssignmentSource, session)?.let { diagnostic ->
        return listOf(diagnostic)
    }

    // 当有明确接收者类型但成员未找到时，优先报告 NOT_MEMBER_OF
    mapNotMemberOfDiagnostic(source, callOrAssignmentSource, session)?.let { diagnostic ->
        return listOf(diagnostic)
    }

    val diagnosticSource = source ?: callOrAssignmentSource ?: return emptyList()
    buildInvalidBinaryOperatorDiagnostic(diagnosticSource, session)?.let { diagnostic ->
        return listOf(diagnostic)
    }

    return listOfNotNull(
        CfirErrors.UNRESOLVED_REFERENCE.on(
            diagnosticSource,
            name.asString(),
            operator,
            session,
        ),
    )
}

/**
 * `[]` / `[]=` 在 resolve 中都会先降成 operator 调用；
 * 这里把针对 `*operator_get` / `*operator_set` 的 unresolved 收束回语法级诊断。
 */
private fun ConeUnresolvedNameError.mapSubscriptOperatorDiagnostic(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    if (operator != "[]") return null
    val diagnosticSource = source ?: callOrAssignmentSource ?: return null
    val receiver = receiverType ?: ConeErrorType(ConeSimpleDiagnostic("unknown subscript receiver type"))

    return if (name == OperatorNameConventions.SET ||
        diagnosticSource.isAssignmentLeftHandSide() ||
        callOrAssignmentSource.isAssignmentExpression()
    ) {
        CfirErrors.CANNOT_ASSIGN_TO_SUBSCRIPT.on(diagnosticSource, session)
    } else {
        CfirErrors.INVALID_SUBSCRIPT_EXPR.on(
            diagnosticSource,
            receiver,
            argumentTypes.joinToString(prefix = "[", postfix = "]") {
                it.renderInvalidBinaryOperatorType(session)
            },
            session,
        )
    }
}

/**
 * 当接收者是类型参数而名称解析失败时，我们优先把它归类为“upper bounds 中没有该成员/方法”，
 * 而不是继续落到通用 unresolved。
 */
private fun ConeUnresolvedNameError.mapGenericUpperBoundAccessDiagnostic(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val typeParameterType = receiverType as? ConeTypeParameterType ?: return null
    val diagnosticSource = source ?: callOrAssignmentSource ?: return null
    val missingName = name
    val typeParameterName = typeParameterType.lookupTag.name

    val hostText = callOrAssignmentSource?.text?.toString().orEmpty()
    val looksLikeMethodCall = argumentTypes.isNotEmpty() || hostText.contains("${missingName.asString()}(")

    return if (!looksLikeMethodCall) {
        CfirErrors.GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS.on(
            diagnosticSource,
            missingName,
            typeParameterName,
            session,
        )
    } else {
        CfirErrors.GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS.on(
            diagnosticSource,
            missingName,
            typeParameterName,
            session,
        )
    }
}

/**
 * 当接收者类型存在且非类型参数时，将 unresolved name 映射为 NOT_MEMBER_OF。
 *
 * 对齐 C++ sema_not_member_of: 'xxx' is not a member of 'Yyy'。
 */
private fun ConeUnresolvedNameError.mapNotMemberOfDiagnostic(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val receiver = receiverType ?: return null
    // 类型参数接收者已由 mapGenericUpperBoundAccessDiagnostic 处理
    if (receiver is ConeTypeParameterType) return null
    // 二元运算符已由 buildInvalidBinaryOperatorDiagnostic 处理
    if (operator != null) return null

    val diagnosticSource = source ?: callOrAssignmentSource ?: return null
    val typeName = receiver.classIdOrPrimitiveClassId?.shortClassName ?: return null
    val kind = if (argumentTypes.isNotEmpty()) "method" else "member"

    return CfirErrors.NOT_MEMBER_OF.on(
        diagnosticSource,
        name,
        kind,
        typeName,
        session,
    )
}

/**
 * 一元运算符解析失败：有 operator 和 receiverType 但无参数。
 *
 * 对齐 C++ sema_invalid_unary_expr。
 */
private fun ConeUnresolvedNameError.mapInvalidUnaryExprDiagnostic(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val operatorToken = operator ?: return null
    val type = receiverType ?: return null
    // 一元运算符：有 operator + receiverType 但无参数
    if (argumentTypes.isNotEmpty()) return null

    val diagnosticSource = source ?: callOrAssignmentSource ?: return null
    return CfirErrors.INVALID_UNARY_EXPR.on(
        diagnosticSource,
        operatorToken,
        type,
        session,
    )
}

private fun ConeUnresolvedNameError.mapExtendSuperDiagnostic(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = source ?: callOrAssignmentSource ?: return null
    val psi = diagnosticSource.psi ?: return null
    val unresolvedName = name.asString()
    val sourceText = psi.text
    if (unresolvedName != "super" && unresolvedName != "<super>" && !sourceText.contains("super")) {
        return null
    }
    if (PsiTreeUtil.getParentOfType(psi, CjExtend::class.java, false) == null) {
        return null
    }

    return CfirErrors.EXTEND_SUPER_NOT_ALLOWED.on(diagnosticSource, session)
}

private fun ConeUnresolvedNameError.buildInvalidBinaryOperatorDiagnostic(
    diagnosticSource: CjSourceElement,
    session: CfirSession,
): CjDiagnostic? {
    val operatorToken = operator ?: return null
    val leftType = receiverType ?: return null
    val rightType = argumentTypes.singleOrNull() ?: return null

    return CfirErrors.INVALID_BINARY_OPERATOR.on(
        diagnosticSource,
        operatorToken,
        leftType.renderInvalidBinaryOperatorType(session),
        rightType.renderInvalidBinaryOperatorType(session),
        session,
    )
}

private fun ConeVisibilityError.mapConeVisibilityError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = source ?: callOrAssignmentSource ?: return null

    // 可见性失败来自解析阶段候选筛选，这里只负责把已有 cone 诊断稳定映射到前端诊断。
    val invisibleSymbol = symbol
    val invisibleName = when (invisibleSymbol) {
        is CfirCallableSymbol<*> -> invisibleSymbol.name.asString()
        is CfirClassLikeSymbol<*> -> invisibleSymbol.classId.shortClassName.asString()
        else -> invisibleSymbol.toString()
    }
    val visibilityText = when (invisibleSymbol) {
        is CfirCallableSymbol<*> -> invisibleSymbol.cfir.status.visibility.externalDisplayName
        is CfirClassLikeSymbol<*> -> invisibleSymbol.visibilityDisplayName()
        else -> "invisible"
    }
    val isMemberAccess = when (invisibleSymbol) {
        is CfirCallableSymbol<*> -> session.cfirProvider.getContainingClass(invisibleSymbol) != null
        else -> false
    }

    return if (isMemberAccess) {
        CfirErrors.INVISIBLE_MEMBER.on(diagnosticSource, invisibleName, visibilityText, session)
    } else {
        CfirErrors.INVISIBLE_REFERENCE.on(diagnosticSource, invisibleName, visibilityText, session)
    }
}

private fun CfirClassLikeSymbol<*>.visibilityDisplayName(): String {
    return when (val declaration = cfir) {
        is CfirClass -> declaration.status.visibility.externalDisplayName
        is CfirInterface -> declaration.status.visibility.externalDisplayName
        is CfirStruct -> declaration.status.visibility.externalDisplayName
        is CfirEnum -> declaration.status.visibility.externalDisplayName
        is CfirTypeAlias -> declaration.status.visibility.externalDisplayName
        else -> "invisible"
    }
}

private fun ConeDiagnostic.mapOtherDiagnostic(
    source: CjSourceElement?,
    valueParameter: CfirValueParameter?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = callOrAssignmentSource ?: source ?: return null
    val genericCallSource = callOrAssignmentSource ?: source
    val genericCallDiagnosticSource = genericCallSource
        ?.genericInferenceCallCalleeSource()
        ?.takeIf { genericCallSource.isImplicitGenericCallWithoutTypeArguments() }
    return when (this) {
        is ConeFunctionExpectedError,
        is ConeFunctionCallExpectedError,
        -> CfirErrors.INVALID_CALLED_OBJECT.on(diagnosticSource, session)

        is ConeCannotInferGenericFunctionTypeParameterType ->
            CfirErrors.UNABLE_TO_INFER_GENERIC_FUNC.on(
                diagnosticSource,
                session,
            )

        is ConeCannotInferTypeParameterType ->
            CfirErrors.UNABLE_TO_INFER_GENERIC_FUNC.on(
                diagnosticSource,
                session,
            )

        is ConeCannotInferValueParameterType ->
            CfirErrors.LAMBDA_MUST_HAVE_TYPE_ANNOTATION.on(diagnosticSource, session)

        is ConeNoConstructorError,
        is ConeNoImplicitDefaultConstructorOnExpectClass,
        is ConeResolutionToClassifierError,
        -> CfirErrors.NO_CONSTRUCTOR.on(diagnosticSource, session)

        is ConeTypeParameterInQualifiedAccess ->
            CfirErrors.GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT.on(
                diagnosticSource,
                symbol.name,
                session,
            )

        is ConeEnumTypeCannotBeUsedAsConstructorError ->
            CfirErrors.ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR.on(
                diagnosticSource,
                enumName,
                session,
            )

        is ConeNoMatchingInvokeOperatorError -> {
            val invokeDiagnosticSource = source ?: diagnosticSource
            CfirErrors.NO_MATCHING_OPERATOR_INVOKE.on(
                CjOffsetsOnlySourceElement(invokeDiagnosticSource.startOffset, invokeDiagnosticSource.endOffset),
                name.asString(),
                receiverType,
                session,
            )
        }

        is ConeEffectsFeatureDisabledError -> CfirErrors.EFFECTS_FEATURE_DISABLED.on(
            diagnosticSource,
            constructName,
            session,
        )

        is ConeCommandIncompatibleTypeError -> CfirErrors.COMMAND_INCOMPATIBLE_TYPE.on(
            diagnosticSource,
            actualType ?: ConeErrorType(ConeSimpleDiagnostic("unknown effect command type")),
            session,
        )

        is ConeCommandHandleTypeError -> CfirErrors.COMMAND_HANDLE_TYPE_ERROR.on(
            diagnosticSource,
            actualType ?: ConeErrorType(ConeSimpleDiagnostic("unknown handler command type")),
            session,
        )

        is ConeImplicitResumeOutsideHandlerError -> CfirErrors.IMPLICIT_RESUME_OUTSIDE_HANDLER.on(
            diagnosticSource,
            session,
        )

        is ConeResumeNoWithError -> CfirErrors.RESUME_NO_WITH.on(
            diagnosticSource,
            resumptionType,
            session,
        )

        is ConeResumeThrowingMismatchTypeError -> CfirErrors.RESUME_THROWING_MISMATCH_TYPE.on(
            diagnosticSource,
            actualType ?: ConeErrorType(ConeSimpleDiagnostic("unknown resume throwing type")),
            session,
        )

        is ConeMismatchingHandleBlockError -> CfirErrors.MISMATCHING_HANDLE_BLOCK.on(
            diagnosticSource,
            actualType,
            expectedType,
            session,
        )

        is ConeSimpleDiagnostic -> when (kind) {
            DiagnosticKind.CannotInferParameterType -> {
                if (genericCallDiagnosticSource != null) {
                    CfirErrors.NEW_INFERENCE_ERROR.on(
                        genericCallDiagnosticSource,
                        "Inference error: ConstraintMismatch",
                        session,
                    )
                } else {
                    valueParameter
                        ?.typeParameters
                        ?.firstOrNull()
                        ?.symbol
                        ?.let { it as? CfirTypeParameterSymbol }
                        ?.let { CfirErrors.CANNOT_INFER_PARAMETER_TYPE.on(diagnosticSource, it, session) }
                }
            }

            DiagnosticKind.LoopInSupertype ->
                CfirErrors.SUPER_TYPES_SELF_REFERENCE.on(diagnosticSource, diagnosticSource.toApproxTypeName(), session)

            DiagnosticKind.DuplicateSupertype -> null

            DiagnosticKind.GenericTypeWithoutTypeArgument ->
                CfirErrors.GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT.on(
                    diagnosticSource.firstCharacterDiagnosticSource(),
                    diagnosticSource.toApproxTypeName(),
                    session,
                )

            DiagnosticKind.SuperNotAllowed ->
                CfirErrors.EXTEND_SUPER_NOT_ALLOWED.on(diagnosticSource, session)

            DiagnosticKind.JumpOutsideLoop ->
                CfirErrors.INVALID_LOOP_CONTROL.on(source ?: diagnosticSource, session)

            DiagnosticKind.ReturnNotAllowed ->
                CfirErrors.INVALID_RETURN.on(diagnosticSource, session)

            DiagnosticKind.ReturnInStaticInit ->
                CfirErrors.INVALID_RETURN_IN_STATIC_INIT.on(diagnosticSource, session)

            DiagnosticKind.CaptureBeforeInitialization ->
                // CaptureBeforeInitialization 需要变量名，但 ConeSimpleDiagnostic 不携带。
                // 使用 reason 字符串中提取的名称，或者使用结构化 Cone 诊断类。
                null

            DiagnosticKind.ThisTypeNotAllowed ->
                CfirErrors.parse_this_type_not_allow.on(source ?: diagnosticSource, session)

            DiagnosticKind.InvalidThisTypePosition ->
                CfirErrors.INVALID_POSITION_OF_THIS_TYPE.on(source ?: diagnosticSource, session)

            DiagnosticKind.EmptyArrayLiteralTypeUndefined ->
                CfirErrors.ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED.on(diagnosticSource, session)

            else -> null
        } ?: mapSimpleDiagnosticByReason(this, diagnosticSource, session)

        is ConeUnresolvedNameError -> CfirErrors.UNRESOLVED_REFERENCE.on(
            diagnosticSource,
            name.asString(),
            operator,
            session,
        )

        is ConeUnresolvedReferenceError -> CfirErrors.UNRESOLVED_REFERENCE.on(
            diagnosticSource,
            name.asString(),
            null,
            session,
        )

        is ConeUnresolvedSymbolError -> CfirErrors.UNRESOLVED_REFERENCE.on(
            diagnosticSource,
            classId.asString(),
            null,
            session,
        )

        is ConeUnresolvedTypeQualifierError -> {
            when {
                source?.kind == CjRealSourceElementKind -> {
                    val lastQualifier = this.qualifiers.last()
                    CfirErrors.UNRESOLVED_REFERENCE.createOn(lastQualifier.source, lastQualifier.name.asString(), null, session)
                }
                else -> {
                    CfirErrors.UNRESOLVED_REFERENCE.createOn(source, this.qualifier, null, session)
                }
            }
        }

        // ── resolve 管线补齐映射 ──

        is ConeCannotRefToPackageNameError -> CfirErrors.CANNOT_REF_TO_PKG_NAME.on(
            diagnosticSource, session,
        )

        is ConePackageNameConflictError -> CfirErrors.AMBIGUOUS_USE.on(
            diagnosticSource, packageName, session,
        )

        is ConeGenericTypeInconsistentError -> CfirErrors.GENERIC_TYPE_INCONSISTENT.on(
            diagnosticSource, typeParameterName, session,
        )

        is ConeUnmatchedTypeArgumentsError -> {
            if (actualCount == 0) {
                CfirErrors.GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT.on(
                    diagnosticSource,
                    symbol.name,
                    session,
                )
            } else {
                CfirErrors.GENERIC_ARGUMENT_NO_MATCH.on(
                    diagnosticSource,
                    session,
                )
            }
        }

        is ConeGenericArgumentNoMatchError -> CfirErrors.GENERIC_ARGUMENT_NO_MATCH.on(
            diagnosticSource, session,
        )

        is ConeGenericTypeArgumentNotMatchConstraintError -> CfirErrors.GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT.on(
            diagnosticSource, actualType, upperBound, genericType, session,
        )

        is ConeGenericConstraintNotLooserError -> CfirErrors.GENERIC_CONSTRAINT_NOT_LOOSER.on(
            diagnosticSource, session,
        )

        is ConeGenericInstantiationCausesAmbiguousFunctionsError -> CfirErrors.GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS.on(
            diagnosticSource, instantiation, functionName, session,
        )

        is ConeMeetConstraintIndirectlyError -> CfirErrors.MEET_CONSTRAINT_INDIRECTLY.on(
            diagnosticSource, session,
        )

        is ConeNotMemberOfError -> CfirErrors.NOT_MEMBER_OF.on(
            diagnosticSource, memberName, kind, typeName, session,
        )

        is ConeMemberNotImportedError -> CfirErrors.MEMBER_NOT_IMPORTED.on(
            diagnosticSource, memberName, session,
        )

        is ConeInvalidUnaryExprError -> CfirErrors.INVALID_UNARY_EXPR.on(
            diagnosticSource, operator, type, session,
        )

        is ConeInvalidUnaryExprWithTargetError -> CfirErrors.INVALID_UNARY_EXPR_WITH_TARGET.on(
            diagnosticSource, operator, type, returnType, session,
        )

        is ConeOptionalChainNonOptionalError -> CfirErrors.OPTIONAL_CHAIN_NON_OPTIONAL.on(
            diagnosticSource, type, session,
        )

        is ConeUnableToInferGenericFuncError -> CfirErrors.UNABLE_TO_INFER_GENERIC_FUNC.on(
            diagnosticSource, session,
        )

        is ConeInvalidNodeAfterCheckError -> CfirErrors.INVALID_NODE_AFTER_CHECK.on(
            diagnosticSource, session,
        )

        is ConeInconsistentArrayLiteralElementTypeError -> CfirErrors.INCONSISTENT_ARRAY_LITERAL_ELEMENT_TYPE.on(
            diagnosticSource, session,
        )

        is ConeTypeMismatchError -> CfirErrors.TYPE_MISMATCH.on(
            diagnosticSource, expectedType, actualType, false, session,
        )

        is ConeMismatchedTypesBecauseError -> CfirErrors.MISMATCHED_TYPES_BECAUSE.on(
            diagnosticSource, expectedType, actualType, because, session,
        )

        is ConeMismatchedTypesMultipleAssignError -> CfirErrors.MISMATCHED_TYPES_MULTIPLE_ASSIGN.on(
            diagnosticSource, actualType, session,
        )

        is ConeParamCountMismatchError -> CfirErrors.PARAM_COUNT_MISMATCH.on(
            diagnosticSource, expected, actual, session,
        )

        is ConeCaptureBeforeInitializationError -> CfirErrors.CAPTURE_BEFORE_INITIALIZATION.on(
            diagnosticSource, variableName, session,
        )

        else -> null
    }
}

/**
 * 对 `DiagnosticKind.Other` 的历史理由串做显式映射。
 *
 * 这些 reason 均来自 resolve/raw-cfir 中已有 producer，不是兜底策略：
 * - 只对已知、稳定的 reason 前缀进行映射；
 * - 未命中的 reason 仍保持原行为（不报告）。
 */
private fun mapSimpleDiagnosticByReason(
    diagnostic: ConeSimpleDiagnostic,
    diagnosticSource: CjSourceElement,
    session: CfirSession,
): CjDiagnostic? {
    val reason = diagnostic.reason
    return when {
        reason == "`return` must be used inside a function" ->
            CfirErrors.INVALID_RETURN.on(diagnosticSource, session)

        reason == "Unresolved return type" ||
            reason == "Unresolved function return type" ->
            CfirErrors.UNABLE_TO_INFER_RETURN_TYPE.on(diagnosticSource, session)

        reason == "unsupported declaration for implicit type" ||
            reason == "failed to resolve implicit type" ||
            reason == "recursive implicit type" ->
            CfirErrors.UNABLE_TO_INFER_DECL.on(diagnosticSource, session)

        reason == "type not resolved after transformation" ||
            reason == "No expected type" ->
            CfirErrors.UNABLE_TO_INFER_EXPR.on(diagnosticSource, session)

        reason.startsWith("Callee reference to candidate without return type:") ||
            reason == "non-name reference" ->
            CfirErrors.INVALID_CALLED_OBJECT.on(diagnosticSource, session)

        else -> null
    }
}

private fun ConeCangJieType.substituteTypeVariableTypes(
    candidate: AbstractCallCandidate<*>,
    session: CfirSession,
): ConeCangJieType {
    val substitutor = candidate.system.asReadOnlyStorage()
        .buildAbstractResultingSubstitutor(session.typeContext)
        .asCone()
    return substitutor.substituteOrSelf(this)
}

private fun ConeConstraintSystemHasContradiction.hasGenericInferenceConstraintMismatch(): Boolean {
    val candidateSymbol = candidate.symbol as? CfirCallableSymbol<*> ?: return false
    if (candidateSymbol.cfir.typeParameters.isEmpty()) return false
    if (candidate.hasExplicitTypeArgumentsInCall()) return false

    return candidate.errors.any { it is ConstraintMismatch }
}

private fun ConeConstraintSystemHasContradiction.explicitTypeArgumentConstraintMismatchDiagnostic(
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    if (!candidate.hasExplicitTypeArgumentsInCall()) return null
    val mismatch = candidate.errors
        .filterIsInstance<ConstraintMismatch>()
        .firstOrNull { it.position.from is ConeExplicitTypeParameterConstraintPosition }
        ?: return null
    val explicitPosition = mismatch.position.from as ConeExplicitTypeParameterConstraintPosition
    val diagnosticSource = explicitPosition.typeArgument.source?.firstCharacterDiagnosticSource()
        ?: qualifiedAccessSource?.genericInferenceWholeCallSource()
        ?: candidate.callInfo.callSite.source?.genericInferenceWholeCallSource()
        ?: source?.genericInferenceWholeCallSource()
        ?: return null

    val actualType = mismatch.lowerConeType.substituteTypeVariableTypes(candidate, session)
    val upperBound = mismatch.upperConeType.substituteTypeVariableTypes(candidate, session)
    val genericType = candidate.callableConstraintOwnerType(session) ?: return null

    return CfirErrors.GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT.on(
        diagnosticSource,
        actualType,
        upperBound,
        genericType,
        session,
    )
}

private fun AbstractCallCandidate<*>.callableConstraintOwnerType(session: CfirSession): ConeCangJieType? {
    val callable = symbol as? CfirCallableSymbol<*> ?: return null
    val declaration = callable.cfir
    return when (declaration) {
        is CfirFunction -> {
            val parameterTypes = declaration.valueParameters.map { parameter ->
                val parameterType = parameter.returnTypeRef.coneTypeOrNull ?: return null
                parameterType.substituteTypeVariableTypes(this, session)
            }
            val returnType = declaration.returnTypeRef.coneTypeOrNull ?: return null
            ConeFunctionType(parameterTypes, returnType.substituteTypeVariableTypes(this, session))
        }

        else -> declaration.returnTypeRef.coneTypeOrNull?.substituteTypeVariableTypes(this, session)
    }
}

private fun AbstractCallCandidate<*>.hasGenericInferenceArgumentMismatch(): Boolean {
    val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return false
    if (callableSymbol.cfir.typeParameters.isEmpty()) return false
    if (hasExplicitTypeArgumentsInCall()) return false

    val declaredTypeParameters = callableSymbol.cfir.typeParameters.mapTo(mutableSetOf()) { it.symbol }
    val argumentMismatches = diagnostics.filterIsInstance<ArgumentTypeMismatch>()
        .filter { it.argument !is CfirAnonymousFunctionExpression }
    return argumentMismatches.any { diagnostic ->
        diagnostic.expectedType.referencesDeclaredTypeParameter(declaredTypeParameters)
    } || argumentMismatches.size >= 2
}

private fun ConeConstraintSystemHasContradiction.genericInferenceErrorDiagnostic(
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val candidateSymbol = candidate.symbol as? CfirCallableSymbol<*> ?: return null
    val declaredTypeParameters = candidateSymbol.cfir.typeParameters.mapTo(mutableSetOf()) { it.symbol }
    val candidateTypeVariable = candidate.system.asReadOnlyStorage().allTypeVariables.values.firstOrNull { variable ->
        (variable as? ConeTypeParameterBasedTypeVariable)?.typeParameterSymbol in declaredTypeParameters
    }

    /**
     * 对齐官方仓颉 `DiagnoseForCallInference`：
     * 泛型调用推断失败的主诊断锚点应优先落在被调用函数名本身，
     * 只有拿不到 callee source 时，才退回到外围 source。
     */
    val diagnosticSource = candidateTypeVariable
        ?.let { candidate.sourceOfCallToSymbolWith(it) }
        ?: candidate.callInfo.callSite.genericInferenceCalleeSource()
        ?: source
        ?: qualifiedAccessSource
        ?: return null

    return CfirErrors.NEW_INFERENCE_ERROR.on(
        diagnosticSource,
        "Inference error: ConstraintMismatch",
        session,
    )
}

private fun ConeConstraintSystemHasContradiction.unableToInferGenericFunctionDiagnostic(
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    /**
     * 官方仓颉将泛型调用实参无法推断归一为
     * `unable to infer generic argument of this function`。
     * 这里保留 Kotlin FIR 的 constraint-system 分层，只在诊断表面映射为
     * 仓颉诊断名，并把调用表达式作为默认范围，匹配 LLT 的 inline 夹注格式。
     */
    val diagnosticSource = qualifiedAccessSource?.genericInferenceWholeCallSource()
        ?: candidate.callInfo.callSite.source?.genericInferenceWholeCallSource()
        ?: source?.genericInferenceWholeCallSource()
        ?: return null

    return CfirErrors.UNABLE_TO_INFER_GENERIC_FUNC.on(
        diagnosticSource,
        session,
    )
}

private fun org.cangnova.cangjie.cfir.expressions.CfirExpression.genericInferenceArgumentMismatchDiagnostic(
    session: CfirSession,
): CjDiagnostic? {
    val qualifiedAccess = unwrapWrappedExpression() as? CfirQualifiedAccessExpression ?: return null
    if (qualifiedAccess !is CfirFunctionCall) return null
    if (qualifiedAccess.typeArguments.isNotEmpty()) return null

    val callableSymbol = qualifiedAccess.genericInferenceCallableSymbolOrNull()
    if (callableSymbol != null) {
        if (callableSymbol.cfir.typeParameters.isEmpty()) return null
        val source = qualifiedAccess.genericInferenceCalleeSource() ?: return null
        return CfirErrors.NEW_INFERENCE_ERROR.on(
            source,
            "Inference error: ConstraintMismatch",
            session,
        )
    }

    val errorDiagnostic = (qualifiedAccess.coneTypeOrNull as? ConeErrorType)?.diagnostic
    if (errorDiagnostic !is ConeCannotInferTypeParameterType) return null

    val source = qualifiedAccess.genericInferenceCalleeSource() ?: return null
    return CfirErrors.NEW_INFERENCE_ERROR.on(
        source,
        "Inference error: ConstraintMismatch",
        session,
    )
}

private fun org.cangnova.cangjie.cfir.expressions.CfirExpression.genericInferenceCallTypeMismatchDiagnostic(
    session: CfirSession,
): CjDiagnostic? {
    val qualifiedAccess = unwrapWrappedExpression() as? CfirQualifiedAccessExpression ?: return null
    if (qualifiedAccess.typeArguments.isNotEmpty()) return null

    val callableSymbol = qualifiedAccess.genericInferenceCallableSymbolOrNull() ?: return null
    if (callableSymbol.cfir.typeParameters.isEmpty()) return null

    val diagnostic = (qualifiedAccess.coneTypeOrNull as? ConeErrorType)?.diagnostic ?: return null
    if (diagnostic !is ConeCannotInferGenericFunctionTypeParameterType &&
        diagnostic !is ConeCannotInferTypeParameterType
    ) {
        return null
    }

    val source = qualifiedAccess.genericInferenceCalleeSource() ?: return null
    return CfirErrors.NEW_INFERENCE_ERROR.on(
        source,
        "Inference error: ConstraintMismatch",
        session,
    )
}

private fun ConeCangJieType.renderInvalidBinaryOperatorType(session: CfirSession): String {
    val classId = classIdOrPrimitiveClassId ?: return toString()
    val declaration = runCatching { session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir }.getOrNull()
    val kind = when (declaration) {
        is CfirClass -> "Class"
        is CfirStruct -> "Struct"
        is CfirEnum -> "Enum"
        is CfirInterface -> "Interface"
        is CfirTypeAlias -> "TypeAlias"
        else -> null
    }
    return if (kind != null) "$kind-${classId.shortClassName.asString()}" else classId.shortClassName.asString()
}

private fun typeMismatchDiagnostic(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement? = null,
    expectedType: ConeCangJieType,
    actualType: ConeCangJieType,
    isMismatchDueToNullability: Boolean,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = source ?: return null
    if (expectedType.containsErrorType() || actualType.containsErrorType()) return null
    specificTypeMismatchDiagnostic(
        source = diagnosticSource,
        expectedType = expectedType,
        actualType = actualType,
        session = session,
    )?.let { return it }

    val typeMismatchTarget = diagnosticSource.typeMismatchTarget() ?: callOrAssignmentSource.typeMismatchTarget()
    return when (typeMismatchTarget) {
        is TypeMismatchTarget.ReturnExpression -> CfirErrors.RETURN_TYPE_MISMATCH.on(
            if (expectedType.rangeElementTypeOrNull() != null && actualType.rangeElementTypeOrNull() != null) {
                return CfirErrors.TYPE_MISMATCH.on(
                    diagnosticSource,
                    expectedType,
                    actualType,
                    isMismatchDueToNullability,
                    session,
                )
            } else typeMismatchTarget.expressionSource,
            expectedType,
            actualType,
            isMismatchDueToNullability,
            session,
        )

        TypeMismatchTarget.FieldInitializer -> null

        null -> CfirErrors.TYPE_MISMATCH.on(
            diagnosticSource,
            expectedType,
            actualType,
            isMismatchDueToNullability,
            session,
        )
    }
}

private fun inferredIntoEmptyIntersection(
    source: CjSourceElement?,
    typeVariable: org.cangnova.cangjie.type.model.TypeVariableMarker,
    incompatibleTypes: Collection<ConeCangJieType>,
    causingTypes: Collection<ConeCangJieType>,
    kind: EmptyIntersectionTypeKind,
    isError: Boolean,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = source ?: return null
    val typeVariableText = when (typeVariable) {
        is ConeTypeParameterBasedTypeVariable -> typeVariable.typeParameterSymbol.name.asString()
        else -> typeVariable.toString()
    }
    val causingTypesText = if (incompatibleTypes == causingTypes) {
        ""
    } else {
        ": ${causingTypes.joinToString()}"
    }
    val kindDescription = kind.toDiagnosticDescription()

    return if (isError) {
        CfirErrors.INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION.on(
            diagnosticSource,
            typeVariableText,
            incompatibleTypes,
            kindDescription,
            causingTypesText,
            session,
        )
    } else {
        CfirErrors.INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION.on(
            diagnosticSource,
            typeVariableText,
            incompatibleTypes,
            kindDescription,
            causingTypesText,
            session,
        )
    }
}

private fun EmptyIntersectionTypeKind.toDiagnosticDescription(): String = when (this) {
    EmptyIntersectionTypeKind.MULTIPLE_CLASSES -> "multiple concrete class or struct bounds are incompatible"
    EmptyIntersectionTypeKind.FINAL_CLASS_AND_INTERFACE -> "a final concrete bound is combined with an interface bound"
}

private fun AbstractCallCandidate<*>.sourceOfCallToSymbolWith(typeVariable: org.cangnova.cangjie.type.model.TypeVariableMarker): CjSourceElement? {
    val declaredTypeParameter = (typeVariable as? ConeTypeParameterBasedTypeVariable)?.typeParameterSymbol ?: return null
    var narrowedSource: CjSourceElement? = null

    callInfo.callSite.acceptChildren(object : CfirVisitorVoid() {
        override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
            if (narrowedSource != null) return

            if (element is CfirQualifiedAccessExpression) {
                val symbol = (element.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol as? CfirCallableSymbol<*>
                if (symbol != null && symbol.cfir.typeParameters.any { it.symbol == declaredTypeParameter }) {
                    narrowedSource = element.calleeReference.source
                    return
                }
            }

            element.acceptChildren(this, null)
        }
    })

    return narrowedSource
}

private fun org.cangnova.cangjie.cfir.CfirElement.genericInferenceCalleeSource(): CjSourceElement? {
    val qualifiedAccess = this as? CfirQualifiedAccessExpression ?: return source
    return qualifiedAccess.calleeReference.source ?: qualifiedAccess.source
}

private fun CjSourceElement.genericInferenceCallCalleeSource(): CjSourceElement? {
    val psiSource = when (this) {
        is CjPsiSourceElement -> this
        is CjLightSourceElement -> this.unwrapToCjPsiSourceElement()
        else -> null
    } ?: return this

    val callExpression = psiSource.psi as? CjCallExpression ?: return this
    return callExpression.calleeExpression?.toCjPsiSourceElement() ?: this
}

private fun CjSourceElement.genericInferenceWholeCallSource(): CjSourceElement {
    val psiSource = when (this) {
        is CjPsiSourceElement -> this
        is CjLightSourceElement -> this.unwrapToCjPsiSourceElement()
        else -> null
    } ?: return this

    return psiSource.psi.containingCallExpressionOrNull()?.toCjPsiSourceElement() ?: this
}

private fun CjSourceElement.isImplicitGenericCallWithoutTypeArguments(): Boolean {
    val psiSource = when (this) {
        is CjPsiSourceElement -> this
        is CjLightSourceElement -> this.unwrapToCjPsiSourceElement()
        else -> null
    } ?: return false

    val callExpression = psiSource.psi.containingCallExpressionOrNull() ?: return false
    return callExpression.typeArguments.isEmpty()
}

private fun CjSourceElement?.hasExplicitTypeArgumentsInSource(): Boolean {
    val psiSource = when (this) {
        is CjPsiSourceElement -> this
        is CjLightSourceElement -> this.unwrapToCjPsiSourceElement()
        else -> null
    } ?: return false

    val callExpression = psiSource.psi.containingCallExpressionOrNull() ?: return false
    return callExpression.typeArguments.isNotEmpty()
}

private fun AbstractCallCandidate<*>.hasExplicitTypeArgumentsInCall(): Boolean {
    return callInfo.hasExplicitTypeArguments ||
        (callInfo.callSite as? CfirQualifiedAccessExpression)?.typeArguments?.isNotEmpty() == true ||
        (callInfo.explicitReceiver as? CfirQualifiedAccessExpression)?.typeArguments?.isNotEmpty() == true ||
        callInfo.callSite.source.hasExplicitTypeArgumentsInSource()
}

private fun PsiElement.containingCallExpressionOrNull(): CjCallExpression? =
    this as? CjCallExpression ?: PsiTreeUtil.getParentOfType(this, CjCallExpression::class.java, false)

private fun CfirQualifiedAccessExpression.genericInferenceCallableSymbolOrNull(): CfirCallableSymbol<*>? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirCallableSymbol<*>
        is CfirErrorNamedReference ->
            (reference.diagnostic as? ConeDiagnosticWithSingleCandidate)?.candidateSymbol as? CfirCallableSymbol<*>
        else -> null
    }
}

private fun CfirAnonymousFunction.containsErrorType(): Boolean {
    return returnTypeRef is CfirErrorTypeRef ||
        valueParameters.any { it.returnTypeRef is CfirErrorTypeRef }
}

/**
 * 仓颉官方 InvalidTy 会阻断后续普通类型不匹配诊断；CFIR 在约束系统里保留
 * delegated type 时，也必须按错误类型处理，避免把已报告错误再次映射成 mismatch。
 */
private fun ConeCangJieType.containsErrorType(): Boolean {
    if (this is ConeErrorType || isError) return true
    return typeArguments.any { it.type.containsErrorType() }
}

private fun org.cangnova.cangjie.cfir.expressions.CfirExpression.containsErrorDiagnosticInArgument(): Boolean {
    if (coneTypeOrNull is ConeErrorType) return true
    if (this is CfirDiagnosticHolder) return true
    if (this is CfirResolvable && calleeReference is CfirDiagnosticHolder) return true

    var hasErrorDiagnostic = false
    acceptChildren(object : CfirVisitorVoid() {
        override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
            if (hasErrorDiagnostic) return
            when {
                element is CfirDiagnosticHolder -> hasErrorDiagnostic = true
                element is CfirResolvable && element.calleeReference is CfirDiagnosticHolder -> hasErrorDiagnostic = true
                element is org.cangnova.cangjie.cfir.expressions.CfirExpression &&
                    element.coneTypeOrNull is ConeErrorType -> hasErrorDiagnostic = true
                else -> element.acceptChildren(this, null)
            }
        }
    }, null)
    return hasErrorDiagnostic
}

/**
 * 官方 Cangjie Sema 对同一次调用中的实参类型错误只报告首个不匹配实参。
 * 这里按候选诊断顺序合并参数约束错误，保留后续非参数约束错误继续映射。
 */
private fun List<ConstraintSystemError>.coalesceArgumentConstraintMismatches(): List<ConstraintSystemError> {
    var reportedArgumentMismatch = false
    return filter { error ->
        if (error !is ConstraintMismatch || error.position.from !is ArgumentConstraintPosition<*>) return@filter true
        if (reportedArgumentMismatch) return@filter false
        reportedArgumentMismatch = true
        true
    }
}

/**
 * 官方 Cangjie Sema 对同一调用的期望类型约束只报告一个主类型不匹配。
 * 显式类型实参的 `Array<T>(...)` 等调用可能在约束系统内生成多个同源
 * expected-type mismatch，这里保留首个用户可见主错误。
 */
private fun List<ConstraintSystemError>.coalesceExpectedTypeConstraintMismatches(): List<ConstraintSystemError> {
    var reportedExpectedTypeMismatch = false
    return filter { error ->
        if (error !is ConstraintMismatch || error.position.from !is ConeExpectedTypeConstraintPosition) return@filter true
        if (reportedExpectedTypeMismatch) return@filter false
        reportedExpectedTypeMismatch = true
        true
    }
}

/**
 * 与约束系统错误保持一致：同一候选上的多个 [ArgumentTypeMismatch]
 * 是同一调用参数检查的级联结果，只保留首个作为用户可见主错误。
 */
private fun List<ResolutionDiagnostic>.coalesceArgumentTypeMismatches(): List<ResolutionDiagnostic> {
    if (any { it.isArgumentMappingDiagnostic }) {
        return filter { it !is ArgumentTypeMismatch }
    }

    var reportedArgumentMismatch = false
    return filter { diagnostic ->
        if (diagnostic !is ArgumentTypeMismatch) return@filter true
        if (reportedArgumentMismatch) return@filter false
        reportedArgumentMismatch = true
        true
    }
}

/**
 * 官方 Cangjie 调用检查在进入逐实参类型检查前先验证参数列表形状。
 * 同一候选已经有缺参、多参、命名参数等映射错误时，实参类型不匹配只是
 * 后续本地检查的派生噪声，不能压过用户可见的参数列表诊断。
 */
private val ResolutionDiagnostic.isArgumentMappingDiagnostic: Boolean
    get() = when (this) {
        is ArgumentPassedTwice,
        is MixingNamedAndPositionalArguments,
        is NamedArgumentsNotAllowed,
        is NamedParameterNotFound,
        is NeedNamedArgument,
        is NoValueForParameter,
        is TooManyArguments,
        -> true

        else -> false
    }

private fun TypeParameterMarker.asDeclaredTypeParameterSymbolOrNull(): CfirTypeParameterSymbol? = when (this) {
    is ConeTypeParameterLookupTag -> typeParameterSymbol
    else -> null
}

private fun org.cangnova.cangjie.type.model.TypeVariableMarker.asDeclaredTypeParameterSymbolOrNull(): CfirTypeParameterSymbol? = when (this) {
    is ConeTypeParameterBasedTypeVariable -> typeParameterSymbol
    else -> null
}

private fun CfirBasedSymbol<*>.memberDeclarationNameOrNull(): Name? = when (this) {
    is CfirCallableSymbol<*> -> name
    is CfirClassLikeSymbol<*> -> classId.shortClassName
    else -> null
}

private fun ConeCangJieType.referencesDeclaredTypeParameter(
    declaredTypeParameters: Set<CfirTypeParameterSymbol>,
): Boolean {
    return when (this) {
        is ConeTypeParameterType -> lookupTag.typeParameterSymbol in declaredTypeParameters
        is ConeClassLikeType -> typeArguments.any { projection ->
            val nestedType = projection.type
            nestedType.referencesDeclaredTypeParameter(declaredTypeParameters)
        }
        is ConeTypeAliasType -> expandedType?.referencesDeclaredTypeParameter(declaredTypeParameters) == true
        else -> false
    }
}

private fun AbstractCallCandidate<*>.hasGenericCallNotEnoughTypeInformation(): Boolean {
    val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return false
    if (callableSymbol.cfir.typeParameters.isEmpty()) return false
    if (hasExplicitTypeArgumentsInCall()) return false

    val declaredTypeParameters = callableSymbol.cfir.typeParameters.mapTo(mutableSetOf()) { it.symbol }
    return errors.any { error ->
        val notEnough = error as? NotEnoughInformationForTypeParameter<*> ?: return@any false
        val typeParameterSymbol = notEnough.typeVariable.asDeclaredTypeParameterSymbolOrNull() ?: return@any false
        typeParameterSymbol in declaredTypeParameters
    }
}

private fun AbstractCallCandidate<*>.isImplicitGenericCallWithTypeParameters(): Boolean {
    val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return false
    return callableSymbol.cfir.typeParameters.isNotEmpty() && !hasExplicitTypeArgumentsInCall()
}

private fun AbstractCallCandidate<*>.isImplicitBuiltinArrayConstructorCall(): Boolean {
    val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return false
    return callableSymbol.cfir.origin == CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor &&
        !hasExplicitTypeArgumentsInCall()
}

private fun AbstractCallCandidate<*>.looksLikeImplicitGenericCallInferenceFailure(
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): Boolean {
    val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return false
    if (callableSymbol.cfir.typeParameters.isEmpty()) return false
    if (hasExplicitTypeArgumentsInCall()) return false
    val calleeSource = source ?: return false
    val callSource = qualifiedAccessSource ?: callInfo.callSite.source ?: return false
    if (calleeSource.startOffset == callSource.startOffset && calleeSource.endOffset == callSource.endOffset) return false
    return callSource.startOffset <= calleeSource.startOffset &&
        calleeSource.endOffset <= callSource.endOffset
}

private tailrec fun org.cangnova.cangjie.cfir.expressions.CfirExpression.unwrapWrappedExpression():
    org.cangnova.cangjie.cfir.expressions.CfirExpression = when (this) {
    is CfirWrappedExpression -> expression.unwrapWrappedExpression()
    else -> this
}

private fun CjSourceElement.toApproxTypeName(): Name {
    val rawText = text?.toString().orEmpty()
    val simplified = rawText.substringAfterLast('.').substringBefore('<').substringBefore('&').trim()
    return Name.identifierIfValid(simplified) ?: Name.ERROR_NAME
}

private fun CjSourceElement.firstCharacterDiagnosticSource(): CjOffsetsOnlySourceElement {
    return CjOffsetsOnlySourceElement(startOffset, (startOffset + 1).coerceAtMost(endOffset))
}

private fun CjSourceElement?.isAssignmentExpression(): Boolean {
    val psi = this?.psi
    return psi is CjBinaryExpression && CjPsiUtil.isAssignment(psi)
}

private fun CjSourceElement?.isAssignmentLeftHandSide(): Boolean {
    val psiExpression = this?.psi as? CjExpression ?: return false
    if (psiExpression.getAssignmentByLHS() != null) return true

    val sourceRange = psiExpression.textRange ?: return false
    val assignment = PsiTreeUtil.getParentOfType(psiExpression, CjBinaryExpression::class.java, true) ?: return false
    if (!CjPsiUtil.isAssignment(assignment)) return false
    val lhsRange = assignment.left?.textRange ?: return false
    return lhsRange.startOffset <= sourceRange.startOffset && sourceRange.endOffset <= lhsRange.endOffset
}

private sealed interface TypeMismatchTarget {
    data class ReturnExpression(val expressionSource: AbstractCjSourceElement) : TypeMismatchTarget
    data object FieldInitializer : TypeMismatchTarget
}

private fun CjPsiSourceElement.findTypeMismatchTarget(): TypeMismatchTarget? {
    var current: com.intellij.psi.PsiElement? = psi
    while (current != null) {
        when (current) {
            is CjReturnExpression -> {
                if (PsiTreeUtil.isAncestor(current.returnedExpression, psi, false)) {
                    val expressionSource = (current.returnedExpression as? CjExpression)?.toCjPsiSourceElement()
                        ?: return null
                    return TypeMismatchTarget.ReturnExpression(expressionSource)
                }
            }

            is CjFieldVariable -> if (PsiTreeUtil.isAncestor(current.initializer, psi, false)) {
                return TypeMismatchTarget.FieldInitializer
            }
        }
        current = current.parent
    }
    return null
}

private fun CjSourceElement?.typeMismatchTarget(): TypeMismatchTarget? {
    val psiSource = when (this) {
        is CjPsiSourceElement -> this
        is CjLightSourceElement -> this.unwrapToCjPsiSourceElement()
        else -> null
    }
    return psiSource?.findTypeMismatchTarget()
}

private val ConstraintMismatch.lowerConeType: ConeCangJieType
    get() = lowerType.asCone()

private val ConstraintMismatch.upperConeType: ConeCangJieType
    get() = upperType.asCone()

private fun diagnosticContext(session: CfirSession): DiagnosticContext {
    return object : DiagnosticContext {
        override val languageVersionSettings = session.languageVersionSettings
        override val containingFilePath: String? = null
        override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean = false
    }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A> CjDiagnosticFactory1<A>.on(
    source: AbstractCjSourceElement,
    a: A,
    session: CfirSession,
): CjDiagnostic? = on(source, a, null, diagnosticContext(session))

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun CjDiagnosticFactory0.on(
    source: AbstractCjSourceElement,
    session: CfirSession,
): CjDiagnostic? = on(source, null, diagnosticContext(session))

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A, B> CjDiagnosticFactory2<A, B>.on(
    source: AbstractCjSourceElement,
    a: A,
    b: B,
    session: CfirSession,
): CjDiagnostic? = on(source, a, b, null, diagnosticContext(session))

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun CjDiagnosticFactory2<String, String?>.on(
    source: AbstractCjSourceElement,
    a: String,
    b: String?,
    session: CfirSession,
): CjDiagnostic? = on(source, a, b, null, diagnosticContext(session))

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun CjDiagnosticFactory2<String, String?>.createOn(
    source: AbstractCjSourceElement?,
    a: String,
    b: String?,
    session: CfirSession,
): CjDiagnostic? = on(source.requireNotNull(), a, b, null, diagnosticContext(session))

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A, B, C> CjDiagnosticFactory3<A, B, C>.on(
    source: AbstractCjSourceElement,
    a: A,
    b: B,
    c: C,
    session: CfirSession,
): CjDiagnostic? = on(source, a, b, c, null, diagnosticContext(session))

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A, B, C, D> CjDiagnosticFactory4<A, B, C, D>.on(
    source: AbstractCjSourceElement,
    a: A,
    b: B,
    c: C,
    d: D,
    session: CfirSession,
): CjDiagnostic? = on(source, a, b, c, d, null, diagnosticContext(session))
