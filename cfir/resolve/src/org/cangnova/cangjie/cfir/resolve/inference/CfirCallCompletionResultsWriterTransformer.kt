package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirPropertyAccess
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference

import org.cangnova.cangjie.cfir.resolve.CfirSamResolver
import org.cangnova.cangjie.cfir.resolve.body.buildAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzer
import org.cangnova.cangjie.cfir.resolve.body.CfirReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirErrorReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractTreeTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.IntegerLiteralAndOperatorApproximationTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.ConeTypeApproximator
import org.cangnova.cangjie.cfir.types.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.coneTypeSafe
import org.cangnova.cangjie.cfir.semantics.isSuccess
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.resolve.calls.tower.isSuccess

class CfirCallCompletionResultsWriterTransformer(
    override val session: CfirSession,
    override val scopeSession: ScopeSession,
    private val finalSubstitutor: ConeSubstitutor,
    private val typeCalculator: CfirReturnTypeCalculator,
    private val typeApproximator: ConeTypeApproximator,
    private val dataFlowAnalyzer: CfirDataFlowAnalyzer,
    private val integerOperatorApproximator: IntegerLiteralAndOperatorApproximationTransformer,
    private val samResolver: CfirSamResolver,
    private val context: BodyResolveContext,
    private val mode: Mode = Mode.Normal,
    private var insideAnnotationContext: Boolean = false,
) : CfirAbstractTreeTransformer<ConeCangJieType?>(phase = CfirResolvePhase.BODY_RESOLVE),
    SessionAndScopeSessionHolder {

    override fun transformFunctionCall(functionCall: CfirFunctionCall, data: ConeCangJieType?): CfirExpression {
        return transformQualifiedAccessLike(functionCall)
    }

    override fun transformPropertyAccess(propertyAccess: CfirPropertyAccess, data: ConeCangJieType?): CfirExpression {
        return transformQualifiedAccessLike(propertyAccess)
    }

    override fun transformQualifiedAccess(qualifiedAccess: CfirQualifiedAccess, data: ConeCangJieType?): CfirExpression {
        return transformQualifiedAccessLike(qualifiedAccess)
    }

    override fun transformAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression, data: ConeCangJieType?): CfirExpression {
        anonymousFunctionExpression.transformChildren(this, null)
        finalizeAnonymousFunction(anonymousFunctionExpression.anonymousFunction)
        anonymousFunctionExpression.replaceConeTypeOrNull(
            integerOperatorApproximator.approximateType(buildLambdaType(anonymousFunctionExpression.anonymousFunction), data)
        )
        return anonymousFunctionExpression
    }

    private fun <T> transformQualifiedAccessLike(access: T): T where T : CfirExpression, T : org.cangnova.cangjie.cfir.CfirResolvable {
        access.transformChildren(this, null)

        val calleeReference = access.calleeReference as? CfirNamedReferenceWithCandidate ?: return access
        val candidate = calleeReference.candidate

        val approximatedExplicitReceiver = when (access) {
            is CfirFunctionCall -> access.explicitReceiver?.transformSingle(integerOperatorApproximator, null)
            is CfirQualifiedAccess -> access.explicitReceiver?.transformSingle(integerOperatorApproximator, null)
            is CfirPropertyAccess -> access.explicitReceiver?.transformSingle(integerOperatorApproximator, null)
            else -> null
        }

        when {
            access is CfirFunctionCall && approximatedExplicitReceiver != null -> access.transformExplicitReceiver(integerOperatorApproximator, null)
            access is CfirQualifiedAccess && approximatedExplicitReceiver != null -> access.transformExplicitReceiver(integerOperatorApproximator, null)
            access is CfirPropertyAccess && approximatedExplicitReceiver != null -> access.transformExplicitReceiver(integerOperatorApproximator, null)
        }

        access.replaceCalleeReference(calleeReference.toFinalReference(access))
        access.replaceConeTypeOrNull(integerOperatorApproximator.approximateType(completedResultType(candidate), null))
        runPCLARelatedTasksForCandidate(candidate)

        return access
    }

    private fun runPCLARelatedTasksForCandidate(candidate: Candidate) {
        for (postponedCall in candidate.postponedPCLACalls) {
            postponedCall.expression.transform<CfirElement, ConeCangJieType?>(this, null)
        }

        for (callback in candidate.onPCLACompletionResultsWritingCallbacks) {
            callback(finalSubstitutor)
        }

        for (lambda in candidate.lambdasAnalyzedWithPCLA) {
            finalizeAnonymousFunction(lambda as? CfirFunction ?: continue)
        }
    }

    private fun finalizeAnonymousFunction(function: CfirFunction) {
        val anonymousFunction = function as? CfirAnonymousFunction ?: return
        val initialReturnType = anonymousFunction.returnTypeRef.coneType
        val returnExpressions = dataFlowAnalyzer
            .returnExpressionsOfAnonymousFunction(anonymousFunction)
            .replacePostponedAtomsInReturnExpressions()
        val expectedReturnType = initialReturnType?.let(::finallySubstituteOrSelf)
            ?: returnExpressions.firstNotNullOfOrNull { it.expression.coneTypeOrNull }

        val newData = expectedReturnType?.let(::withExpectedType)
        for (returnExpression in returnExpressions) {
            if (newData != null) {
                returnExpression.expression.transform<CfirElement, ConeCangJieType?>(this, newData)
            } else {
                returnExpression.expression.transform<CfirElement, ConeCangJieType?>(this, null)
            }
            returnExpression.expression.transformSingle(integerOperatorApproximator, newData)
        }

        anonymousFunction.body?.let { body ->
            if (newData != null) {
                body.transform<CfirElement, ConeCangJieType?>(this, newData)
            } else {
                body.transform<CfirElement, ConeCangJieType?>(this, null)
            }
            body.transformSingle(integerOperatorApproximator, newData)
        }

        val resultReturnType = integerOperatorApproximator.approximateType(
            anonymousFunction.body?.coneTypeOrNull ?: expectedReturnType,
            expectedReturnType,
        ) ?: return
        if (initialReturnType != resultReturnType) {
            anonymousFunction.replaceReturnTypeRef(
                anonymousFunction.returnTypeRef.resolvedTypeFromPrototype(resultReturnType, anonymousFunction.source)
            )
        }
    }

    private fun buildLambdaType(function: CfirFunction): ConeCangJieType? {
        val parameterTypes = function.valueParameters.mapNotNull { it.returnTypeRef.coneTypeSafe<ConeCangJieType>() }
        val returnType = function.returnTypeRef.coneTypeSafe<ConeCangJieType>() ?: function.body?.coneTypeOrNull ?: return null
        return ConeFuncType(parameterTypes, returnType)
    }

    private fun withExpectedType(expectedType: ConeCangJieType): ConeCangJieType = expectedType

    private fun Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo>.replacePostponedAtomsInReturnExpressions(): Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo> {
        return map { returnInfo ->
            val replacement = replacePostponedAtom(returnInfo.expression)
            if (replacement === returnInfo.expression) {
                returnInfo
            } else {
                returnInfo.copy(expression = replacement)
            }
        }
    }

    private fun replacePostponedAtom(expression: CfirExpression): CfirExpression {
        val candidate = (expression as? org.cangnova.cangjie.cfir.CfirResolvable)
            ?.calleeReference as? CfirNamedReferenceWithCandidate
            ?: return expression
        val resolvedCandidate = candidate.candidate
        return when {
            expression is CfirQualifiedAccess || expression is CfirPropertyAccess || expression is CfirFunctionCall -> {
                expression.transform<CfirElement, ConeCangJieType?>(this, null) as CfirExpression
            }

            resolvedCandidate.argumentReplacements?.containsKey(expression) == true -> {
                resolvedCandidate.argumentReplacements.getValue(expression)
            }

            else -> expression
        }
    }

    private fun completedResultType(candidate: Candidate): ConeCangJieType {
        val substituted = finallySubstituteOrSelf(candidate.substitutedReturnType())
        val approximated = typeApproximator.approximateToSuperType(substituted) ?: substituted
        return integerOperatorApproximator.approximateType(approximated, null) ?: approximated
    }

    private fun CfirNamedReferenceWithCandidate.toFinalReference(access: CfirExpression): org.cangnova.cangjie.cfir.references.CfirReference {
        val diagnostic = when {
            this is CfirErrorReferenceWithCandidate -> diagnostic
            !candidate.lowestApplicability.isSuccess -> ConeInapplicableCandidateError(candidate.lowestApplicability, candidate)
            !candidate.isSuccessful -> ConeSimpleDiagnostic("Candidate constraint system contradiction: ${name.asString()}")
            else -> null
        }

        if (diagnostic != null) {
            return when (this) {
                is CfirNamedReference -> buildErrorNamedReference {
                    source = this@toFinalReference.source
                    name = this@toFinalReference.name
                    this.diagnostic = diagnostic
                }

                else -> buildErrorReference {
                    source = this@toFinalReference.source
                    reason = diagnostic.reason
                }
            }
        }

        return toResolvedReference(access)
    }

    private fun CfirNamedReferenceWithCandidate.toResolvedReference(access: CfirExpression): org.cangnova.cangjie.cfir.references.CfirReference {
        return when (access) {
            is CfirFunctionCall, is CfirQualifiedAccess, is CfirPropertyAccess ->
                buildAppliedCallableReference(name, candidate, completedResultType(candidate))
            else -> buildResolvedNamedReference {
                source = this@toResolvedReference.source
                name = this@toResolvedReference.name
                resolvedSymbol = this@toResolvedReference.candidateSymbol
            }
        }
    }

    private fun finallySubstituteOrNull(
        type: ConeCangJieType,
        substitutor: ConeSubstitutor = finalSubstitutor,
    ): ConeCangJieType? {
        val result = substitutor.substituteOrNull(type)
        if (result == null && type is ConeIdealLiteralType) {
            return type.approximateIntegerLiteralType()
        }
        return result?.approximateIntegerLiteralType()
    }

    private fun finallySubstituteOrSelf(type: ConeCangJieType): ConeCangJieType {
        return finallySubstituteOrNull(type) ?: type
    }

    enum class Mode {
        Normal,
        // Retained only as an upstream-aligned seam. The current local direct chain has
        // no delegated-property inference session or writer-construction call site that
        // selects this mode, so this enum value is intentionally unreachable for now.
        DelegatedPropertyCompletion,
    }

    private inline fun <T> withCollectionLiteralInAnnotationResolution(block: () -> T): T {
        val savedInsideAnnotationContext = insideAnnotationContext
        insideAnnotationContext = true
        return try {
            block()
        } finally {
            insideAnnotationContext = savedInsideAnnotationContext
        }
    }

    override fun <E : CfirElement> transformElement(element: E, data: ConeCangJieType?): E {
        if (element is CfirDeclaration) return element
        return super.transformElement(element, data)
    }
}

private fun ConeCangJieType.approximateIntegerLiteralType(): ConeCangJieType =
    when (this) {
        is ConeIdealLiteralType -> getApproximatedType()
        else -> this
    }
