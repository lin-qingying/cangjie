package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostic.AmbiguousArgumentType
import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.resolve.calls.*
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeRegularLambdaArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaParameterType
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeTypeIntersector
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.isSubtypeConstraintCompatible
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE
import org.cangnova.cangjie.resolve.calls.inference.model.ArgumentConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.source.CjSourceElement

internal object ArgumentCheckingProcessor {
    private data class ArgumentContext(
        val candidate: Candidate,
        val csBuilder: ConstraintSystemBuilder,
        val expectedType: ConeCangJieType?,
        val sink: CheckerSink?,
        val context: ResolutionContext,
        val isReceiver: Boolean,
        val isDispatch: Boolean,
        val anonymousFunctionIfReturnExpression: CfirAnonymousFunction? = null,
    ) : SessionHolder {
        override val session: CfirSession
            get() = context.session

        fun reportDiagnostic(diagnostic: ResolutionDiagnostic) {
            sink?.reportDiagnostic(diagnostic)
        }
    }

    fun resolveArgumentExpression(
        candidate: Candidate,
        atom: ConeResolutionAtom,
        expectedType: ConeCangJieType?,
        sink: CheckerSink,
        context: ResolutionContext,
        isReceiver: Boolean,
        isDispatch: Boolean,
        anonymousFunctionIfReturnExpression: CfirAnonymousFunction? = null,
    ) {
        ArgumentContext(
            candidate = candidate,
            csBuilder = candidate.system.getBuilder(),
            expectedType = expectedType,
            sink = sink,
            context = context,
            isReceiver = isReceiver,
            isDispatch = isDispatch,
            anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
        ).resolveArgumentExpression(atom)
    }

    fun resolvePlainArgumentType(
        candidate: Candidate,
        atom: ConeResolutionAtom,
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType?,
        sink: CheckerSink,
        context: ResolutionContext,
        isReceiver: Boolean,
        isDispatch: Boolean,
        sourceForReceiver: CjSourceElement? = null,
    ) {
        ArgumentContext(
            candidate = candidate,
            csBuilder = candidate.system.getBuilder(),
            expectedType = expectedType,
            sink = sink,
            context = context,
            isReceiver = isReceiver,
            isDispatch = isDispatch,
        ).resolvePlainArgumentType(atom, argumentType, sourceForReceiver)
    }

    fun createResolvedLambdaAtomDuringCompletion(
        candidate: Candidate,
        csBuilder: ConstraintSystemBuilder,
        atom: ConeResolutionAtomWithPostponedChild,
        expectedType: ConeCangJieType?,
        context: ResolutionContext,
        returnTypeVariable: ConeTypeVariableForLambdaReturnType?,
        anonymousFunctionIfReturnExpression: CfirAnonymousFunction? = null,
    ): ConeResolvedLambdaAtom {
        return ArgumentContext(
            candidate = candidate,
            csBuilder = csBuilder,
            expectedType = expectedType,
            sink = null,
            context = context,
            isReceiver = false,
            isDispatch = false,
            anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
        ).createResolvedLambdaAtom(atom, duringCompletion = true, returnTypeVariable = returnTypeVariable)
    }

    private fun ArgumentContext.resolveArgumentExpression(atom: ConeResolutionAtom) {
        when (atom) {
            is ConeResolutionAtomWithPostponedChild -> when (atom.expression) {
                is CfirAnonymousFunctionExpression -> preprocessLambdaArgument(atom)
                is CfirNamedAccessExpression -> preprocessFunctionReferenceArgument(atom, atom.expression)
                else -> {
                    atom.useFallbackSubAtom()
                    val child = atom.subAtom
                    if (child != null) {
                        resolveArgumentExpression(child)
                    } else {
                        resolvePlainExpressionArgument(atom)
                    }
                }
            }

            is ConeResolutionAtomWithSingleChild -> {
                val child = atom.subAtom
                if (child != null) {
                    resolveArgumentExpression(child)
                } else {
                    resolvePlainExpressionArgument(atom)
                }
            }

            is ConeSimpleLeafResolutionAtom,
            is ConeAtomWithCandidate -> resolvePlainExpressionArgument(atom)

            is ConePostponedResolvedAtom -> Unit
        }
    }

    private fun ArgumentContext.preprocessFunctionReferenceArgument(
        atom: ConeResolutionAtomWithPostponedChild,
        expression: CfirNamedAccessExpression,
    ) {
        val fallback = atom.fallbackSubAtom ?: run {
            atom.useFallbackSubAtom()
            resolvePlainExpressionArgument(atom)
            return
        }
        val targetExpectedType = expectedType
        if (targetExpectedType == null) {
            atom.useFallbackSubAtom()
            resolveArgumentExpression(fallback)
            return
        }

        val postponedAtom = ConeSimpleNameForContextSensitiveResolution(
            expression = expression,
            expectedType = targetExpectedType,
            containingCallCandidate = candidate,
            fallbackSubAtom = fallback,
        )
        atom.setPostponedSubAtom(postponedAtom)
        candidate.addPostponedAtom(postponedAtom)
    }

    private fun ArgumentContext.resolvePlainExpressionArgument(atom: ConeResolutionAtom) {
        val argumentType = atom.expression.coneTypeOrNull ?: return
        resolvePlainArgumentType(atom, argumentType)
    }
    private fun  ArgumentContext.createArgumentConstraintPosition(atom: ConeResolutionAtom): ArgumentConstraintPosition<*> {
        return when (val containingLambda = anonymousFunctionIfReturnExpression) {
            null -> ConeArgumentConstraintPosition(atom.expression)
            else -> ConeRegularLambdaArgumentConstraintPosition(containingLambda, atom.expression)
        }
    }

    private fun ArgumentContext.resolvePlainArgumentType(
        atom: ConeResolutionAtom,
        argumentType: ConeCangJieType,
        sourceForReceiver: CjSourceElement? = null,
    ) {
        val position = when {
//            isReceiver -> ConeReceiverConstraintPosition(expression, sourceForReceiver)
            else -> createArgumentConstraintPosition(atom)
        }
        val preparedType = prepareArgumentType(argumentType, context.session)
        checkApplicabilityForArgumentType(atom, preparedType, position)
    }
    private fun ArgumentContext.checkApplicabilityForArgumentType(
        atom: ConeResolutionAtom,
        argumentTypeBeforeCapturing: ConeCangJieType,
        position: ConstraintPosition,
    ) {
        if (expectedType == null) return

        val argumentType = substituteTypeParameterUpperBoundIfNeeded(argumentTypeBeforeCapturing, expectedType, session)
        val normalizedArgumentType = normalizeTypeForCompatibilityCheck(argumentType)
        val expression = atom.expression

        fun subtypeError(actualExpectedType: ConeCangJieType): ResolutionDiagnostic {
            fun tryGetConeTypeThatCompatibleWithKtType(type: ConeCangJieType): ConeCangJieType {
                if (type is ConeTypeVariableType) {
                    val lookupTag = type.typeConstructor

                    val constraints = csBuilder.currentStorage().notFixedTypeVariables[lookupTag]?.constraints
                    val constraintTypes = constraints?.mapNotNull { it.type as? ConeCangJieType }
                    if (!constraintTypes.isNullOrEmpty()) {
                        return ConeTypeIntersector.intersectTypes(session.typeContext, constraintTypes)
                    }

                    val originalTypeParameter = lookupTag.originalTypeParameter as? ConeTypeParameterLookupTag
                    if (originalTypeParameter != null) {
                        return ConeTypeParameterTypeImpl(originalTypeParameter , type.attributes)
                    }
                } else if (type is ConeIdealLiteralType) {
                    return type.defaultType
                }

                return type
            }

            if (argumentType is ConeErrorType || actualExpectedType is ConeErrorType) return ErrorTypeInArguments

            val preparedExpectedType = tryGetConeTypeThatCompatibleWithKtType(actualExpectedType)
            val preparedActualType = tryGetConeTypeThatCompatibleWithKtType(argumentType)
            return ArgumentTypeMismatch(
                preparedExpectedType,
                preparedActualType,
                expression,
                false,
                anonymousFunctionIfReturnExpression,
                csBuilder.hasContradiction,
            )
        }

        val compatible = csBuilder.isSubtypeConstraintCompatible(normalizedArgumentType, expectedType)
        csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
        if (!compatible) {
            reportDiagnostic(subtypeError(expectedType))
        }
    }
    private fun  ArgumentContext.shouldRunConversion(): Boolean {
        // Currently, we only apply conversions for arguments, not lambda's return expressions
//        if (anonymousFunctionIfReturnExpression != null) {
//            // For latest LV it's equal to `return false`
//            return !LanguageFeature.DoNotRunSuspendConversionForLambdaReturnStatements.isEnabled()
//        }
        return true
    }

    private fun ArgumentContext.preprocessLambdaArgument(atom: ConeResolutionAtomWithPostponedChild) {
        if (createLambdaWithTypeVariableAsExpectedTypeAtomIfNeeded(atom)) return
        createResolvedLambdaAtom(atom, duringCompletion = false, returnTypeVariable = null)
    }

    private fun ArgumentContext.createLambdaWithTypeVariableAsExpectedTypeAtomIfNeeded(
        atom: ConeResolutionAtomWithPostponedChild,
    ): Boolean {
        val expectedType = expectedType as? ConeTypeVariableType ?: return false
        val explicitTypeArgument = csBuilder.currentStorage()
            .notFixedTypeVariables[expectedType.typeConstructor]
            ?.constraints
            ?.find { constraint ->
                constraint.kind == ConstraintKind.EQUALITY &&
                    constraint.position.from is ConeExplicitTypeParameterConstraintPosition
            }
            ?.type as? ConeCangJieType

        if (explicitTypeArgument != null && explicitTypeArgument.typeArguments.isEmpty()) {
            return false
        }

        val lambdaAtom = ConeLambdaWithTypeVariableAsExpectedTypeAtom(
            expression = atom.lambdaExpression,
            anonymousFunction = atom.lambdaExpression.anonymousFunction,
            expectedType = expectedType,
            candidateOfOuterCall = candidate,
            anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
        )
        candidate.addPostponedAtom(lambdaAtom)
        atom.setPostponedSubAtom(lambdaAtom)
        return true
    }

    private fun ArgumentContext.createResolvedLambdaAtom(
        atom: ConeResolutionAtomWithPostponedChild,
        duringCompletion: Boolean,
        returnTypeVariable: ConeTypeVariableForLambdaReturnType?,
    ): ConeResolvedLambdaAtom {
        val expression = atom.lambdaExpression
        val anonymousFunction = expression.anonymousFunction
        val expectedFunctionType = expectedType as? ConeFunctionType

        val parameterTypes = anonymousFunction.valueParameters.mapIndexed { index, parameter ->
            parameter.returnTypeRef.coneTypeOrNull
                ?: expectedFunctionType?.parameterTypes?.getOrNull(index)
                ?: ConeTypeVariableForLambdaParameterType(
                    TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + index,
                ).also(csBuilder::registerVariable).defaultType
        }

        val createdReturnTypeVariable = if (anonymousFunction.returnTypeRef.coneTypeOrNull == null &&
            returnTypeVariable == null &&
            expectedFunctionType == null
        ) {
            ConeTypeVariableForLambdaReturnType(anonymousFunction, TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE)
                .also(csBuilder::registerVariable)
        } else {
            null
        }

        val lambdaReturnType = anonymousFunction.returnTypeRef.coneTypeOrNull
            ?: returnTypeVariable?.defaultType
            ?: expectedFunctionType?.returnType
            ?: createdReturnTypeVariable!!.defaultType

        val resolvedAtom = ConeResolvedLambdaAtom(
            expression = expression,
            anonymousFunction = anonymousFunction,
            expectedType = expectedType,
            parameterTypes = parameterTypes,
            returnType = lambdaReturnType,
            typeVariableForLambdaReturnType = returnTypeVariable ?: createdReturnTypeVariable,
        )

        atom.setPostponedSubAtom(resolvedAtom)
        candidate.addPostponedAtom(resolvedAtom)

        val targetExpectedType = expectedType
        if (targetExpectedType != null) {
            val lambdaType = ConeFunctionType(parameterTypes = parameterTypes, returnType = lambdaReturnType)
            val position = ConeArgumentConstraintPosition(expression)
            if (duringCompletion) {
                csBuilder.addSubtypeConstraint(lambdaType, targetExpectedType, position)
            } else {
                val compatible = csBuilder.isSubtypeConstraintCompatible(lambdaType, targetExpectedType)
                csBuilder.addSubtypeConstraint(lambdaType, targetExpectedType, position)
                if (!compatible) {
                    if (targetExpectedType !is ConeErrorType) {
                        reportDiagnostic(
                            ArgumentTypeMismatch(
                                expectedType = targetExpectedType,
                                actualType = lambdaType,
                                argument = expression,
                                isMismatchDueToNullability = false,
                                anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
                                systemHadContradiction = csBuilder.hasContradiction,
                            ),
                        )
                    }
                }
            }
        }

        return resolvedAtom
    }

    private val ConeResolutionAtomWithPostponedChild.lambdaExpression: CfirAnonymousFunctionExpression
        get() = expression as? CfirAnonymousFunctionExpression
            ?: error("Expected anonymous function expression, but was ${expression::class}")
}
