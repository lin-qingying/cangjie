package org.cangnova.cangjie.cfir.analysis.diagnostics

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferTypeParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeConstraintSystemHasContradiction
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeNoConstructorError
import org.cangnova.cangjie.cfir.diagnostic.ConeNoMatchingInvokeOperatorError
import org.cangnova.cangjie.cfir.diagnostic.ConeResolutionToClassifierError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedSymbolError
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
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjReturnExpression
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
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
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
    val genericDiagnostic = source?.let {
        CfirErrors.UNRESOLVED_REFERENCE.on(it, candidateSymbol.debugName, null, session)
    }

    val diagnostics = candidate.diagnostics.filter { !it.isSuccess }.mapNotNull { rootCause ->
        when (rootCause) {
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

            else -> genericDiagnostic
        }
    }

    if (diagnostics.isNotEmpty()) return diagnostics

    val diagnosticSource = qualifiedAccessSource ?: source ?: return emptyList()
    return listOfNotNull(CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, candidateSymbol.debugName, null, session))
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

    if (anonymousFunction != null) {
        if (session.languageVersionSettings.supportsFeature(LanguageFeature.LambdaReturnTypeMismatchAsArgumentTypeMismatch)) {
            val lambdaSource = anonymousFunction.source ?: source
            return CfirErrors.ARGUMENT_TYPE_MISMATCH.on(
                lambdaSource,
                expectedType,
                actualType,
                isMismatchDueToNullability,
                session,
            )
        }

        return CfirErrors.RETURN_TYPE_MISMATCH.on(
            source,
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
                return sharedDiagnostics
            }

            return diagnosticsByKey.first()
        }
    }

    val diagnosticSource = callOrAssignmentSource ?: source ?: return emptyList()
    return listOfNotNull(CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, name.asString(), null, session))
}

private data class DiagnosticIdentityKey(
    val factoryName: String,
    val message: String,
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

private fun ConeUnresolvedNameError.mapConeUnresolvedNameError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): List<CjDiagnostic> {
    val diagnosticSource = callOrAssignmentSource ?: source ?: return emptyList()
    return listOfNotNull(
        CfirErrors.UNRESOLVED_REFERENCE.on(
            diagnosticSource,
            name.asString(),
            operator,
            session,
        ),
        buildInvalidBinaryOperatorDiagnostic(diagnosticSource, session),
    )
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

private fun ConeDiagnostic.mapOtherDiagnostic(
    source: CjSourceElement?,
    valueParameter: CfirValueParameter?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = callOrAssignmentSource ?: source ?: return null
    return when (this) {
        is ConeCannotInferTypeParameterType ->
            CfirErrors.CANNOT_INFER_PARAMETER_TYPE.on(diagnosticSource, typeParameter, session)

        is ConeNoConstructorError,
        is ConeResolutionToClassifierError,
        -> CfirErrors.NO_CONSTRUCTOR.on(diagnosticSource, session)

        is ConeNoMatchingInvokeOperatorError -> CfirErrors.NO_MATCHING_OPERATOR_INVOKE.on(
            diagnosticSource, name.asString(), receiverType, session
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

            else -> null
        }

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

private fun CfirSymbol<*>.memberDeclarationNameOrNull(): Name? = when (this) {
    is CfirCallableSymbol<*> -> name
    is CfirClassLikeSymbol<*> -> classId.shortClassName
    else -> null
}

private fun CjSourceElement.toApproxTypeName(): Name {
    val rawText = text?.toString().orEmpty()
    val simplified = rawText.substringAfterLast('.').substringBefore('<').substringBefore('&').trim()
    return Name.identifierIfValid(simplified) ?: Name.ERROR_NAME
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
