package org.cangnova.cangjie.cfir.analysis.diagnostics

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.ArgumentPassedTwice
import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferTypeParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferValueParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotRefToPackageNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeConstraintSystemHasContradiction
import org.cangnova.cangjie.cfir.diagnostic.ConeCommandHandleTypeError
import org.cangnova.cangjie.cfir.diagnostic.ConeCommandIncompatibleTypeError
import org.cangnova.cangjie.cfir.diagnostic.ConeEnumTypeCannotBeUsedAsConstructorError
import org.cangnova.cangjie.cfir.diagnostic.ConeEffectsFeatureDisabledError
import org.cangnova.cangjie.cfir.diagnostic.ConeFunctionCallExpectedError
import org.cangnova.cangjie.cfir.diagnostic.ConeFunctionExpectedError
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeImplicitResumeOutsideHandlerError
import org.cangnova.cangjie.cfir.diagnostic.ConeMismatchingHandleBlockError
import org.cangnova.cangjie.cfir.diagnostic.ConeNoConstructorError
import org.cangnova.cangjie.cfir.diagnostic.ConeNoImplicitDefaultConstructorOnExpectClass
import org.cangnova.cangjie.cfir.diagnostic.ConeNoMatchingInvokeOperatorError
import org.cangnova.cangjie.cfir.diagnostic.ConeResolutionToClassifierError
import org.cangnova.cangjie.cfir.diagnostic.ConeResumeNoWithError
import org.cangnova.cangjie.cfir.diagnostic.ConeResumeThrowingMismatchTypeError
import org.cangnova.cangjie.cfir.diagnostic.ConeTypeParameterInQualifiedAccess
import org.cangnova.cangjie.cfir.diagnostic.ConeVisibilityError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedSymbolError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedTypeQualifierError
import org.cangnova.cangjie.cfir.diagnostic.ConeGenericTypeInconsistentError
import org.cangnova.cangjie.cfir.diagnostic.ConeGenericArgumentNoMatchError
import org.cangnova.cangjie.cfir.diagnostic.ConeGenericConstraintNotLooserError
import org.cangnova.cangjie.cfir.diagnostic.ConeGenericInstantiationCausesAmbiguousFunctionsError
import org.cangnova.cangjie.cfir.diagnostic.ConeMeetConstraintIndirectlyError
import org.cangnova.cangjie.cfir.diagnostic.ConeNotMemberOfError
import org.cangnova.cangjie.cfir.diagnostic.ConeMemberNotImportedError
import org.cangnova.cangjie.cfir.diagnostic.ConeInvalidUnaryExprError
import org.cangnova.cangjie.cfir.diagnostic.ConeInvalidUnaryExprWithTargetError
import org.cangnova.cangjie.cfir.diagnostic.ConeOptionalChainNonOptionalError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnableToInferGenericFuncError
import org.cangnova.cangjie.cfir.diagnostic.ConeInvalidNodeAfterCheckError
import org.cangnova.cangjie.cfir.diagnostic.ConeMismatchedTypesBecauseError
import org.cangnova.cangjie.cfir.diagnostic.ConeMismatchedTypesMultipleAssignError
import org.cangnova.cangjie.cfir.diagnostic.ConeParamCountMismatchError
import org.cangnova.cangjie.cfir.diagnostic.ConeCaptureBeforeInitializationError
import org.cangnova.cangjie.cfir.diagnostic.MixingNamedAndPositionalArguments
import org.cangnova.cangjie.cfir.diagnostic.NamedArgumentsNotAllowed
import org.cangnova.cangjie.cfir.diagnostic.NamedParameterNotFound
import org.cangnova.cangjie.cfir.diagnostic.NeedNamedArgument
import org.cangnova.cangjie.cfir.diagnostic.NoValueForParameter
import org.cangnova.cangjie.cfir.diagnostic.TooManyArguments
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.diagnostics.InternalDiagnosticFactoryMethod
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory0
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory1
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory2
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory3
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory4
import org.cangnova.cangjie.cfir.diagnostics.requireNotNull
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeLambdaArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeReceiverConstraintPosition
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.isSuccess
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjBinaryExpression
import org.cangnova.cangjie.psi.CjPsiUtil
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjReturnExpression
import org.cangnova.cangjie.psi.psiUtil.getAssignmentByLHS
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintMismatch
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.inference.model.ConstrainingTypeIsError
import org.cangnova.cangjie.resolve.calls.inference.model.FixVariableConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.InferredEmptyIntersection
import org.cangnova.cangjie.resolve.calls.inference.model.InferredEmptyIntersectionError
import org.cangnova.cangjie.resolve.calls.inference.model.MultiLambdaBuilderInferenceRestriction
import org.cangnova.cangjie.resolve.calls.inference.model.NotEnoughInformationForTypeParameter
import org.cangnova.cangjie.resolve.calls.inference.model.OnlyInputTypesDiagnostic
import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeKind
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.type.model.TypeParameterMarker

fun ConeDiagnostic.toCfirDiagnostics(
    session: CfirSession,
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    valueParameter: CfirValueParameter? = null,
): List<CjDiagnostic> {
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
            typeVariable.asDeclaredTypeParameterSymbolOrNull()?.let {
                CfirErrors.CANNOT_INFER_PARAMETER_TYPE.on(
                    source ?: qualifiedAccessSource ?: candidate.callInfo.callSite.source ?: return null,
                    it,
                    session,
                )
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

        is MultiLambdaBuilderInferenceRestriction<*> -> {
            val anonymousFunction = anonymous as? CfirAnonymousFunction ?: return null
            val typeParameterSymbol = typeParameter.asDeclaredTypeParameterSymbolOrNull() ?: return null
            val containingDeclarationName = typeParameterSymbol.containingDeclarationSymbol.memberDeclarationNameOrNull()
                ?: error("containingDeclarationSymbol must have been a member declaration")
            CfirErrors.BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION.on(
                anonymousFunction.source ?: source ?: qualifiedAccessSource ?: return null,
                typeParameterSymbol.name,
                containingDeclarationName,
                session,
            )
        }

        else -> null
    }
}

private fun ConeConstraintSystemHasContradiction.mapSystemHasContradictionError(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): List<CjDiagnostic> {
    val errors = candidate.errors
    if (hasGenericInferenceConstraintMismatch()) {
        return listOfNotNull(genericInferenceErrorDiagnostic(source, qualifiedAccessSource, session))
    }

    val hasNotEnoughInformationError = errors.any { it is NotEnoughInformationForTypeParameter<*> }
    val mismatchTarget = (qualifiedAccessSource ?: source).typeMismatchTarget()
    return errors.mapNotNull { error ->
        if (hasNotEnoughInformationError &&
            error is ConstraintMismatch &&
            error.position.from is ConeExpectedTypeConstraintPosition &&
            mismatchTarget !is TypeMismatchTarget.PatternInitializer
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

private fun ConeInapplicableCandidateError.mapInapplicableCandidateError(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): List<CjDiagnostic> {
    val noMatchingInvokeDiagnostic = mapNoMatchingInvokeOperatorDiagnostic(session, source, qualifiedAccessSource)
    val genericDiagnostic = (qualifiedAccessSource ?: source)?.let { diagnosticSource ->
        when (candidateSymbol.cfir) {
            is org.cangnova.cangjie.cfir.declarations.CfirConstructor,
            is CfirEnumConstructor,
            -> CfirErrors.NO_CONSTRUCTOR.on(diagnosticSource, session)

            else -> CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, candidateSymbol.debugName, null, session)
        }
    }

    val diagnostics = candidate.diagnostics.filter { !it.isSuccess }.mapNotNull { rootCause ->
        when (rootCause) {
            is ArgumentPassedTwice -> CfirErrors.ARGUMENT_PASSED_TWICE.on(
                rootCause.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
                session,
            )

            is ArgumentTypeMismatch -> {
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
                )
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

    noMatchingInvokeDiagnostic?.let { return listOf(it) }

    val diagnosticSource = qualifiedAccessSource ?: source ?: return emptyList()
    return listOfNotNull(CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, candidateSymbol.debugName, null, session))
}

private fun ConeInapplicableCandidateError.mapNoMatchingInvokeOperatorDiagnostic(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): CjDiagnostic? {
    if (candidateSymbol.memberDeclarationNameOrNull() != OperatorNameConventions.INVOKE) return null
    val receiverType = candidate.callInfo.explicitReceiver?.coneTypeOrNull ?: return null
    if (receiverType is ConeErrorType) return null
    val diagnosticSource = source ?: qualifiedAccessSource ?: return null
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
    specificTypeMismatchDiagnostic(
        source = source,
        expectedType = expectedType,
        actualType = actualType,
        session = session,
    )?.let { return it }

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

private fun ConeAmbiguityError.mapConeAmbiguityError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): List<CjDiagnostic> {
    if (candidateSymbols.all { symbol ->
            symbol.cfir is org.cangnova.cangjie.cfir.declarations.CfirConstructor || symbol.cfir is CfirEnumConstructor
        }
    ) {
        val diagnosticSource = callOrAssignmentSource ?: source ?: return emptyList()
        return listOfNotNull(CfirErrors.AMBIGUOUS_CONSTRUCTOR_CALL.on(diagnosticSource, name, session))
    }

    @OptIn(ApplicabilityDetail::class)
    if (!applicability.isSuccess) {
        val candidateDiagnostics = candidatesWithErrors.values.map { coneDiagnostic ->
            coneDiagnostic?.toCfirDiagnostics(
                session = session,
                source = source,
                callOrAssignmentSource = null,
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

    val factory = if (isCallLikeContext) CfirErrors.AMBIGUOUS_FUNCTION_CALL else CfirErrors.AMBIGUOUS_USE
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

    val diagnosticSource = callOrAssignmentSource ?: source ?: return emptyList()
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
    if (!session.languageVersionSettings.supportsFeature(LanguageFeature.InvalidBinaryOperatorDiagnostics)) return null

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
    val declaration = cfir
    return when (declaration) {
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
    return when (this) {
        is ConeFunctionExpectedError,
        is ConeFunctionCallExpectedError,
        -> CfirErrors.INVALID_CALLED_OBJECT.on(diagnosticSource, session)

        is ConeCannotInferTypeParameterType ->
            CfirErrors.CANNOT_INFER_PARAMETER_TYPE.on(diagnosticSource, typeParameter, session)

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

        is ConeNoMatchingInvokeOperatorError -> CfirErrors.NO_MATCHING_OPERATOR_INVOKE.on(
            diagnosticSource, name.asString(), receiverType, session
        )

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
                valueParameter
                    ?.typeParameters
                    ?.firstOrNull()
                    ?.symbol
                    ?.let { it as? CfirTypeParameterSymbol }
                    ?.let { CfirErrors.CANNOT_INFER_PARAMETER_TYPE.on(diagnosticSource, it, session) }
            }

            DiagnosticKind.LoopInSupertype ->
                CfirErrors.SUPER_TYPES_SELF_REFERENCE.on(diagnosticSource, diagnosticSource.toApproxTypeName(), session)

            DiagnosticKind.DuplicateSupertype -> null

            DiagnosticKind.GenericTypeWithoutTypeArgument ->
                CfirErrors.GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT.on(
                    diagnosticSource,
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

        is ConeGenericTypeInconsistentError -> CfirErrors.GENERIC_TYPE_INCONSISTENT.on(
            diagnosticSource, typeParameterName, session,
        )

        is ConeGenericArgumentNoMatchError -> CfirErrors.GENERIC_ARGUMENT_NO_MATCH.on(
            diagnosticSource, session,
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

    return candidate.errors.any { error ->
        error is ConstraintMismatch && when (error.position.from) {
            is ConeArgumentConstraintPosition,
            is ConeLambdaArgumentConstraintPosition,
            is ConeReceiverConstraintPosition -> false

            else -> true
        }
    }
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

    val diagnosticSource = candidateTypeVariable
        ?.let { candidate.sourceOfCallToSymbolWith(it) }
        ?: source
        ?: qualifiedAccessSource
        ?: return null

    return CfirErrors.NEW_INFERENCE_ERROR.on(
        diagnosticSource,
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
    specificTypeMismatchDiagnostic(
        source = diagnosticSource,
        expectedType = expectedType,
        actualType = actualType,
        session = session,
    )?.let { return it }

    val typeMismatchTarget = diagnosticSource.typeMismatchTarget() ?: callOrAssignmentSource.typeMismatchTarget()
    return when (typeMismatchTarget) {
        is TypeMismatchTarget.ReturnExpression -> CfirErrors.RETURN_TYPE_MISMATCH.on(
            typeMismatchTarget.expressionSource,
            expectedType,
            actualType,
            isMismatchDueToNullability,
            session,
        )

        TypeMismatchTarget.PatternInitializer -> CfirErrors.PATTERN_INITIALIZER_TYPE_MISMATCH.on(
            diagnosticSource,
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

private fun CfirAnonymousFunction.containsErrorType(): Boolean {
    return returnTypeRef is CfirErrorTypeRef ||
        valueParameters.any { it.returnTypeRef is CfirErrorTypeRef }
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

private fun CjSourceElement.toApproxTypeName(): Name {
    val rawText = text?.toString().orEmpty()
    val simplified = rawText.substringAfterLast('.').substringBefore('<').substringBefore('&').trim()
    return Name.identifierIfValid(simplified) ?: Name.ERROR_NAME
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
    data object PatternInitializer : TypeMismatchTarget
    data object FieldInitializer : TypeMismatchTarget
}

private fun CjPsiSourceElement.findTypeMismatchTarget(): TypeMismatchTarget? {
    var current = psi
    while (current != null) {
        when (current) {
            is CjReturnExpression -> {
                if (PsiTreeUtil.isAncestor(current.returnedExpression, psi, false)) {
                    val expressionSource = (current.returnedExpression as? CjExpression)?.toCjPsiSourceElement()
                        ?: return null
                    return TypeMismatchTarget.ReturnExpression(expressionSource)
                }
            }

            is CjPatternVariable -> if (PsiTreeUtil.isAncestor(current.initializer, psi, false)) {
                return TypeMismatchTarget.PatternInitializer
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
