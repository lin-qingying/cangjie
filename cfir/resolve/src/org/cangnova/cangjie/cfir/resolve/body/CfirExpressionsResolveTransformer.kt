package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirNamedReferenceImpl
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.typeFromCallee
import org.cangnova.cangjie.cfir.resolve.withExpectedType
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeNoMatchingInvokeOperatorError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSpecificTypeResolverTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.resultType
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.whileAnalysing
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.isSuccess

/**
 * Expression resolve transformer.
 *
 * Responsibility: compute and propagate expression types only.
 * This includes literals, accesses, calls, patterns, control-flow, and lambdas.
 *
 * Diagnostic reporting is intentionally NOT performed here. Resolution keeps
 * candidate diagnostics attached to the resolver/completion pipeline output,
 * and a dedicated checker pass reports them after body resolve completes.
 */
@OptIn(CfirImplementationDetail::class, ApplicabilityDetail::class)
class CfirExpressionsResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
) : CfirPartialBodyResolveTransformer(transformer) {

    private val builtinTypes get() = session.builtinTypes
    private val specificTypeResolverTransformer = CfirSpecificTypeResolverTransformer(session)
    private val callResolver get() = components.callResolver
    private fun errorType(
        reason: String,
        kind: DiagnosticKind = DiagnosticKind.Other,
        delegatedType: ConeCangJieType? = null,
    ): ConeErrorType = ConeErrorType(ConeSimpleDiagnostic(reason, kind), delegatedType = delegatedType)

    init {
        components.callResolver.initTransformer(this)
    }

    // ── Literals ─────────────────────────────────────────────────────────────

    override fun transformExpression(expression: CfirExpression, data: ResolutionMode): CfirExpression {
        if (!expression.hasResolvedType && expression !is CfirWrappedExpression) {
            expression.resultType = ConeErrorType(
                ConeSimpleDiagnostic(
                    "Type calculating for ${expression::class} is not supported",
                    DiagnosticKind.InferenceError
                )
            )
        }
        return (expression.transformChildren(transformer, data) as CfirExpression)
    }

    private fun transformThisReceiverExpression(
        thisReceiverExpression: CfirThisReceiverExpression,
        data: ResolutionMode,
    ): CfirExpression {
        thisReceiverExpression.transformAnnotations(transformer, data)

        if (thisReceiverExpression.coneTypeOrNull == null) {
            val thisReference = thisReceiverExpression.calleeReference
            val resultType = components.typeFromCallee(thisReference)
            thisReceiverExpression.replaceConeTypeOrNull(resultType)
            thisReference.replaceDiagnostic((resultType as? ConeErrorType)?.diagnostic)

            if (thisReference.boundSymbol == null && resultType !is ConeErrorType) {
                components.implicitValueStorage[null].singleOrNull()?.let { implicitReceiver ->
                    thisReference.replaceBoundSymbol(implicitReceiver.boundSymbol)
                }
            }
        }

        return thisReceiverExpression
    }

    override fun transformSuperReceiverExpression(
        superReceiverExpression: CfirSuperReceiverExpression,
        data: ResolutionMode,
    ): CfirExpression {
        superReceiverExpression.transformAnnotations(transformer, data)

        val superReference = superReceiverExpression.calleeReference
        val resolvedSuperTypeRef = resolveSuperTypeRef(superReference.superTypeRef)
        if (resolvedSuperTypeRef !== superReference.superTypeRef) {
            superReference.replaceSuperTypeRef(resolvedSuperTypeRef)
        }

        val owner = context.containingRegularClass
        val receiverType = when {
            owner == null -> errorType("`super` is only allowed inside class declarations")
            resolvedSuperTypeRef is CfirResolvedTypeRef -> resolveExplicitSuperReceiverType(owner, resolvedSuperTypeRef)
            else -> resolveImplicitSuperReceiverType(owner)
        }

        superReceiverExpression.replaceConeTypeOrNull(receiverType)
        return superReceiverExpression
    }

    override fun transformLiteralExpression(
        literalExpression: CfirLiteralExpression,
        data: ResolutionMode,
    ): CfirExpression {
        val synthesized = synthesizeLiteralType(literalExpression.kind)
        val expectedType = data.expectedTypeOrNull
        literalExpression.replaceConeTypeOrNull(IdealTypeResolver.resolveIfIdeal(synthesized, expectedType))
        return literalExpression
    }

    private fun synthesizeLiteralType(kind: CfirLiteralKind): ConeCangJieType = when (kind) {
        CfirLiteralKind.INT     -> ConePrimitiveType.IDEAL_INT
        CfirLiteralKind.FLOAT   -> ConePrimitiveType.IDEAL_FLOAT
        CfirLiteralKind.BOOLEAN -> builtinTypes.boolType
        CfirLiteralKind.RUNE    -> ConePrimitiveType.RUNE
        CfirLiteralKind.STRING  -> stdlibStringType()
        CfirLiteralKind.UNIT    -> builtinTypes.unitType
    }

    // ── Named Access ─────────────────────────────────────────────────────────

    override fun transformNamedAccessExpression(
        namedAccessExpression: CfirNamedAccessExpression,
        data: ResolutionMode,
    ): CfirExpression =
        transformQualifiedAccessExpression(
            qualifiedAccessExpression = namedAccessExpression,
            data = data,
            isUsedAsReceiver = data is ResolutionMode.ReceiverResolution,
            isUsedAsGetClassReceiver = false,
        )

    // ── Qualified Access ──────────────────────────────────────────────────────

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        data: ResolutionMode,
    ): CfirExpression =
        transformQualifiedAccessExpression(
            qualifiedAccessExpression = qualifiedAccessExpression,
            data = data,
            isUsedAsReceiver = data is ResolutionMode.ReceiverResolution,
            isUsedAsGetClassReceiver = false,
        )

    private fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        data: ResolutionMode,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
    ): CfirExpression =
        whileAnalysing(session, qualifiedAccessExpression) {
            val calleeReference = qualifiedAccessExpression.calleeReference

            if (qualifiedAccessExpression.coneTypeOrNull != null && calleeReference !is CfirNamedReferenceImpl) {
                return@whileAnalysing qualifiedAccessExpression
            }

            qualifiedAccessExpression.transformAnnotations(transformer, data)
            resolveAccessTypeArguments(qualifiedAccessExpression)

            when (qualifiedAccessExpression.calleeReference) {
                is CfirThisReference -> {
                    if (qualifiedAccessExpression.coneTypeOrNull == null) {
                        val resultType = components.typeFromCallee(qualifiedAccessExpression)
                        qualifiedAccessExpression.replaceConeTypeOrNull(resultType)
                        (qualifiedAccessExpression.calleeReference as? CfirThisReference)
                            ?.replaceDiagnostic((resultType as? ConeErrorType)?.diagnostic)
                    }
                    qualifiedAccessExpression
                }

                is CfirResolvedNamedReference,
                is CfirErrorNamedReference,
                -> {
                    if (qualifiedAccessExpression.coneTypeOrNull == null) {
                        storeTypeFromCallee(qualifiedAccessExpression)
                    }
                    qualifiedAccessExpression
                }

                is CfirNamedReference -> {
                    val transformedCallee = resolveQualifiedAccessAndSelectCandidate(
                        qualifiedAccessExpression = qualifiedAccessExpression,
                        isUsedAsReceiver = isUsedAsReceiver,
                        isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
                        callSite = qualifiedAccessExpression,
                        data = data,
                    )
                    if (transformedCallee is CfirQualifiedAccessExpression) {
                        val candidateReference = transformedCallee.calleeReference as? CfirNamedReferenceWithCandidate
                        if (candidateReference != null) {
                            completeResolvedAccess(transformedCallee, data)
                        } else {
                            when (transformedCallee.calleeReference) {
                                is CfirResolvedNamedReference,
                                is CfirErrorNamedReference,
                                is CfirThisReference,
                                -> {
                                    if (transformedCallee.coneTypeOrNull == null) {
                                        storeTypeFromCallee(transformedCallee)
                                    }
                                    transformedCallee
                                }

                                else -> transformedCallee
                            }
                        }
                    } else {
                        transformedCallee
                    }
                }

                else -> {
                    qualifiedAccessExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
                    if (qualifiedAccessExpression.coneTypeOrNull == null) {
                        qualifiedAccessExpression.replaceConeTypeOrNull(
                            ConeErrorType(ConeSimpleDiagnostic("non-name reference", DiagnosticKind.Other))
                        )
                    }
                    qualifiedAccessExpression
                }
            }
        }

    // ── Function Call ─────────────────────────────────────────────────────────

    override fun transformFunctionCall(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirExpression =
        transformFunctionCallInternal(functionCall, data, CallResolutionMode.REGULAR)

    internal fun transformFunctionCallInternal(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
        callResolutionMode: CallResolutionMode,
    ): CfirExpression =
        whileAnalysing(session, functionCall) {
            val calleeReference = functionCall.calleeReference
            if (
                (calleeReference is CfirResolvedNamedReference || calleeReference is CfirErrorNamedReference) &&
                functionCall.coneTypeOrNull == null
            ) {
                storeTypeFromCallee(functionCall)
            }
            if (calleeReference is CfirNamedReferenceWithCandidate) return@whileAnalysing functionCall
            if (calleeReference !is CfirNamedReferenceImpl) {
                if (calleeReference !is CfirResolvedNamedReference) {
                    functionCall.transformChildren(transformer, ResolutionMode.ContextIndependent)
                }
                return@whileAnalysing functionCall
            }

            functionCall.transformAnnotations(transformer, data)
            resolveAccessTypeArguments(functionCall)

            val choosingOptionForAugmentedAssignment = callResolutionMode == CallResolutionMode.OPTION_FOR_AUGMENTED_ASSIGNMENT
            val withTransformedArguments = if (!choosingOptionForAugmentedAssignment) {
                components.dataFlowAnalyzer.enterCallArguments(functionCall, functionCall.argumentList.arguments)

                val withResolvedExplicitReceiver = when (callResolutionMode) {
                    CallResolutionMode.PROVIDE_DELEGATE -> functionCall
                    else -> transformExplicitReceiverOf(functionCall)
                }

                withResolvedExplicitReceiver.also {
                    components.dataFlowAnalyzer.exitCallExplicitReceiver()
                    it.replaceArgumentList(
                        it.argumentList.transform(transformer, ResolutionMode.ContextDependent)
                    )
                    components.dataFlowAnalyzer.exitCallArguments()
                }
            } else {
                functionCall
            }

            // 保存原始引用，resolveCallAndSelectCandidate 会原地修改 calleeReference
            val originalCalleeReference = withTransformedArguments.calleeReference
            val resolvedCall = callResolver.resolveCallAndSelectCandidate(withTransformedArguments, data)
            val callForCompletion = if (!choosingOptionForAugmentedAssignment) {
                tryResolveImplicitInvokeCall(originalCalleeReference, withTransformedArguments, resolvedCall, data) ?: resolvedCall
            } else {
                resolvedCall
            }

            if (!choosingOptionForAugmentedAssignment) {
                components.dataFlowAnalyzer.enterFunctionCall(callForCompletion)
            }

            val result = components.callCompleter.completeCall(
                callForCompletion,
                data,
                skipEvenPartialCompletion = choosingOptionForAugmentedAssignment,
            )

            if (!choosingOptionForAugmentedAssignment) {
                components.dataFlowAnalyzer.exitFunctionCall(result, data.forceFullCompletion)
            }

            result
        }

    private fun tryResolveImplicitInvokeCall(
        originalCalleeReference: CfirReference,
        originalCall: CfirFunctionCall,
        resolvedCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirFunctionCall? {
        if (originalCall.explicitReceiver != null) return null

        val diagnostic = (resolvedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic
        val shouldPreserveOriginalDiagnostic = diagnostic !is ConeUnresolvedNameError
        val canTryImplicitInvoke = when (diagnostic) {
            is ConeUnresolvedNameError -> true
            is ConeInapplicableCandidateError -> diagnostic.candidateSymbol is CfirEnumConstructorSymbol
            is ConeAmbiguityError -> !diagnostic.applicability.isSuccess &&
                    diagnostic.candidateSymbols.all { it is CfirEnumConstructorSymbol }
            else -> false
        }
        if (!canTryImplicitInvoke) return null

        val originalCallee = originalCalleeReference as? CfirNamedReferenceImpl ?: return null
        if (originalCallee.name == OperatorNameConventions.INVOKE) return null

        val resolvedAccess = callResolver.resolveNamedValueAccessAndSelectCandidate(
            qualifiedAccess = buildNamedAccessExpression {
                source = originalCall.source
                calleeReference = buildNamedReference {
                    source = originalCallee.source
                    name = originalCallee.name
                }
                typeArguments.addAll(originalCall.typeArguments)
            },
            isUsedAsReceiver = true,
            isUsedAsGetClassReceiver = false,
            callSite = originalCall,
            resolutionMode = data,
        ) as? CfirQualifiedAccessExpression ?: return null

        when (resolvedAccess.calleeReference) {
            is CfirResolvedNamedReference,
            is CfirNamedReferenceWithCandidate,
            -> Unit

            else -> return null
        }

        val invokeCall = buildFunctionCall {
            source = originalCall.source
            calleeReference = buildNamedReference {
                source = originalCallee.source
                name = OperatorNameConventions.INVOKE
            }
            explicitReceiver = resolvedAccess
            argumentList = buildArgumentList {
                arguments.addAll(originalCall.argumentList.arguments)
            }
            typeArguments.addAll(originalCall.typeArguments)
            origin = originalCall.origin
        }

        val invokeResult = callResolver.resolveCallAndSelectCandidate(invokeCall, data)
            .takeUnless { (it.calleeReference as? CfirDiagnosticHolder)?.diagnostic is ConeUnresolvedNameError }

        if (invokeResult != null) return invokeResult

        if (shouldPreserveOriginalDiagnostic) return null

        // 变量已解析但类型上没有 invoke 操作符 → 报告专用诊断
        val receiverType = resolvedAccess.coneTypeOrNull
        if (receiverType != null && receiverType !is ConeErrorType) {
            resolvedCall.replaceCalleeReference(
                buildErrorNamedReference {
                    source = originalCallee.source
                    name = originalCallee.name
                    this.diagnostic = ConeNoMatchingInvokeOperatorError(originalCallee.name, receiverType)
                }
            )
            return resolvedCall
        }

        return null
    }

    private fun storeTypeFromCallee(functionCall: CfirFunctionCall) {
        storeTypeFromCallee(functionCall as CfirQualifiedAccessExpression)
    }

    internal fun storeTypeFromCallee(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        @Suppress("UNUSED_PARAMETER") isLhsOfAssignment: Boolean = false,
    ) {
        qualifiedAccessExpression.replaceConeTypeOrNull(components.typeFromCallee(qualifiedAccessExpression))
    }

    fun <Q : CfirQualifiedAccessExpression> transformExplicitReceiverOf(qualifiedAccessExpression: Q): Q {
        if (qualifiedAccessExpression.explicitReceiver == null) return qualifiedAccessExpression
        qualifiedAccessExpression.transformExplicitReceiver(transformer, ResolutionMode.ReceiverResolution)
        return qualifiedAccessExpression
    }

    protected open fun resolveQualifiedAccessAndSelectCandidate(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
        callSite: CfirElement,
        data: ResolutionMode,
    ): CfirExpression {
        return callResolver.resolveNamedValueAccessAndSelectCandidate(
            qualifiedAccess = qualifiedAccessExpression,
            isUsedAsReceiver = isUsedAsReceiver,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            callSite = callSite,
            resolutionMode = data,
        )
    }

    internal enum class CallResolutionMode {
        REGULAR,

        /**
         * For PROVIDE_DELEGATE we skip transforming explicit receiver of the call since it's already been resolved
         * at [FirDeclarationsResolveTransformer.transformPropertyAccessorsWithDelegate]
         */
        PROVIDE_DELEGATE,

        /**
         * When we're resolving an operator like `a += b` we try to resolve it with different options of desugaring like
         * `a = a.plus(b)` and `a.plusAssign(b)` until find something that looks successful.
         * But at this stage, we skip transformation of receiver, arguments and skip completion in any form.
         */
        OPTION_FOR_AUGMENTED_ASSIGNMENT,
    }

    // ── Block ─────────────────────────────────────────────────────────────────

    override fun transformBlock(block: CfirBlock, data: ResolutionMode): CfirExpression {
        block.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val lastExpr = block.statements.lastOrNull()
        block.replaceConeTypeOrNull(
            if (lastExpr is CfirExpression) lastExpr.coneTypeOrNull ?: builtinTypes.unitType
            else builtinTypes.unitType
        )
        return block
    }

    // ── Match ─────────────────────────────────────────────────────────────────

    override fun transformMatchExpression(
        matchExpression: CfirMatchExpression,
        data: ResolutionMode,
    ): CfirExpression {
        matchExpression.subject?.resolveIndependently()
        val subjectType = matchExpression.subject?.coneTypeOrNull

        val branchTypes = matchExpression.branches.map { branch ->
            resolveBranch(branch, subjectType)
        }

        matchExpression.replaceConeTypeOrNull(computeMatchResultType(branchTypes))
        return matchExpression
    }

    private fun resolveBranch(
        branch: CfirMatchBranch,
        subjectType: ConeCangJieType?,
    ): ConeCangJieType {
        return withNewLocalScope {
            branch.transformPattern(transformer, ResolutionMode.ContextIndependent)
            resolvePattern(branch.pattern, subjectType)

            branch.transformGuard(transformer, ResolutionMode.ContextIndependent)
            branch.transformBody(transformer, ResolutionMode.ContextIndependent)

            val bodyType = branch.body.coneTypeOrNull ?: builtinTypes.unitType
            branch.replaceConeTypeOrNull(bodyType)
            bodyType
        }
    }

    private fun resolvePattern(pattern: CfirPattern, expectedType: ConeCangJieType?) {
        when (pattern) {
            is CfirWildcardPattern  -> Unit
            is CfirConstPattern     -> Unit
            is CfirExpressionPattern -> Unit

            is CfirBindingPattern -> {
                val bindingType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType ?: expectedType
                if (bindingType != null) storePatternBinding(pattern.name, bindingType)
                pattern.nestedPattern?.let { resolvePattern(it, expectedType) }
            }

            is CfirTuplePattern -> {
                val tupleType = expectedType as? ConeTupleType
                pattern.elements.forEachIndexed { i, sub ->
                    resolvePattern(sub, tupleType?.elementTypes?.getOrNull(i))
                }
            }

            is CfirEnumPattern -> pattern.arguments.forEach { resolvePattern(it, null) }

            is CfirTypePattern -> {
                val patternType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType
                if (patternType != null && pattern.bindingName != null) {
                    storePatternBinding(pattern.bindingName!!, patternType)
                }
            }

            is CfirOrPattern -> pattern.alternatives.forEach { alt ->
                withNewLocalScope { resolvePattern(alt, expectedType) }
            }
        }
    }

    private fun storePatternBinding(name: Name, type: ConeCangJieType) {
        val symbol = CfirFieldVariableSymbol(CallableId(name))
        buildFieldVariable {
            this.name = name
            this.symbol = symbol
            this.moduleData = context.file.moduleData
            this.origin = CfirDeclarationOrigin.Source
            this.attributes = CfirDeclarationAttributes.EMPTY
            this.status = CfirDeclarationStatusImpl()
            this.returnTypeRef = type.toCfirResolvedTypeRef()
            this.isVar = false
        }
        context.storeVariable(name, symbol)
    }

    private fun computeMatchResultType(branchTypes: List<ConeCangJieType>): ConeCangJieType = when {
        branchTypes.isEmpty()                        -> builtinTypes.unitType
        branchTypes.size == 1                        -> branchTypes.single()
        branchTypes.all { it == branchTypes.first() } -> branchTypes.first()
        else                                         -> ConeUnionType(branchTypes.toSet())
    }

    // ── If ────────────────────────────────────────────────────────────────────

    override fun transformIfExpression(
        ifExpression: CfirIfExpression,
        data: ResolutionMode,
    ): CfirExpression {
        ifExpression.transformCondition(transformer, withExpectedType(builtinTypes.boolType))
        val branchResolutionMode = (data as? ResolutionMode.WithExpectedType)
            ?.takeUnless { it.fromCast }
            ?.copy(forceFullCompletion = false)
            ?: ResolutionMode.ContextDependent

        ifExpression.transformThenBranch(transformer, branchResolutionMode)
        ifExpression.transformElseBranch(transformer, branchResolutionMode)

        val thenType = ifExpression.thenBranch.coneTypeOrNull
        val elseType = ifExpression.elseBranch?.coneTypeOrNull
        val mergedType = when {
            thenType == null -> elseType ?: builtinTypes.unitType
            elseType == null -> builtinTypes.unitType
            thenType == elseType -> thenType
            else -> commonSupertype(listOf(thenType, elseType))
        }
        ifExpression.replaceConeTypeOrNull(
            IdealTypeResolver.resolveIfIdeal(mergedType, data.expectedTypeOrNull)
        )
        return ifExpression
    }

    // ── Return / Throw ────────────────────────────────────────────────────────

    override fun transformReturnExpression(
        returnExpression: CfirReturnExpression,
        data: ResolutionMode,
    ): CfirExpression {
        returnExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        returnExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        return returnExpression
    }

    override fun transformThrowExpression(
        throwExpression: CfirThrowExpression,
        data: ResolutionMode,
    ): CfirExpression {
        throwExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        throwExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        return throwExpression
    }

    // ── Assignment ────────────────────────────────────────────────────────────

    override fun transformAssignment(
        assignment: CfirAssignment,
        data: ResolutionMode,
    ): CfirExpression {
        val subscriptLValue = assignment.lValue as? CfirSubscriptExpression
        if (subscriptLValue != null) {
            assignment.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
            subscriptLValue.transformReceiver(transformer, ResolutionMode.ContextIndependent)
            subscriptLValue.transformIndices(transformer, ResolutionMode.ContextIndependent)
            assignment.transformRValue(transformer, ResolutionMode.ContextIndependent)

            resolveSubscriptSetAssignment(assignment, subscriptLValue, data)
            assignment.replaceConeTypeOrNull(builtinTypes.unitType)
            return assignment
        }

        assignment.transformChildren(transformer, ResolutionMode.ContextIndependent)
        assignment.replaceConeTypeOrNull(builtinTypes.unitType)
        return assignment
    }

    // ── Tuple / Array / String Literals ──────────────────────────────────────

    override fun transformTupleLiteral(
        tupleLiteral: CfirTupleLiteral,
        data: ResolutionMode,
    ): CfirExpression {
        tupleLiteral.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val elementTypes = tupleLiteral.elements.map {
            it.coneTypeOrNull ?: errorType("unresolved element")
        }
        tupleLiteral.replaceConeTypeOrNull(ConeTupleType(elementTypes))
        return tupleLiteral
    }

    override fun transformArrayLiteral(
        arrayLiteral: CfirArrayLiteral,
        data: ResolutionMode,
    ): CfirExpression {
        arrayLiteral.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val elementType = arrayLiteral.elements.firstNotNullOfOrNull { it.coneTypeOrNull }
            ?: errorType("empty array literal")
        arrayLiteral.replaceConeTypeOrNull(
            constructNamedType(
                classId = StdlibClassIds.Array,
                typeArguments = listOf(ConeTypeProjection(elementType)),
            )
        )
        return arrayLiteral
    }

    override fun transformStringInterpolation(
        stringInterpolation: CfirStringInterpolation,
        data: ResolutionMode,
    ): CfirExpression {
        stringInterpolation.transformChildren(transformer, ResolutionMode.ContextIndependent)
        stringInterpolation.replaceConeTypeOrNull(stdlibStringType())
        return stringInterpolation
    }

    // ── Comparison / Binary / Type Operators ──────────────────────────────────

    override fun transformComparisonExpression(
        comparisonExpression: CfirComparisonExpression,
        data: ResolutionMode,
    ): CfirExpression {
        comparisonExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        comparisonExpression.replaceConeTypeOrNull(resolveComparisonExpressionType(comparisonExpression, data))
        return comparisonExpression
    }

    private fun resolveComparisonExpressionType(
        comparisonExpression: CfirComparisonExpression,
        data: ResolutionMode,
    ): ConeCangJieType {
        val leftType = comparisonExpression.left.coneTypeOrNull
        val rightType = comparisonExpression.right.coneTypeOrNull
        if (leftType == null || rightType == null) return builtinTypes.boolType

        val operatorName = comparisonExpression.operation.toOperatorName()
        CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
            operatorName,
            leftType,
            listOf(rightType),
        )?.let { return it.returnType }

        val comparisonCall = buildFunctionCall {
            source = comparisonExpression.source
            calleeReference = buildNamedReference {
                source = comparisonExpression.source
                name = operatorName
            }
            explicitReceiver = comparisonExpression.left
            argumentList = buildArgumentList {
                source = comparisonExpression.source
                arguments.add(comparisonExpression.right)
            }
            origin = CfirFunctionCallOrigin.Operator
        }

        val resolvedCall = callResolver.resolveCallAndSelectCandidate(comparisonCall, data)
        (resolvedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            return ConeErrorType(diagnostic, delegatedType = builtinTypes.boolType)
        }

        val completedCall = components.callCompleter.completeCall(resolvedCall, data)
        (completedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            return ConeErrorType(diagnostic, delegatedType = builtinTypes.boolType)
        }

        return completedCall.coneTypeOrNull ?: builtinTypes.boolType
    }

    override fun transformBinaryOp(
        binaryOp: CfirBinaryOp,
        data: ResolutionMode,
    ): CfirExpression {
        binaryOp.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val resultType = when (binaryOp.kind) {
            CfirBinaryOpKind.AND, CfirBinaryOpKind.OR -> builtinTypes.boolType
            CfirBinaryOpKind.COALESCING               -> binaryOp.left.coneTypeOrNull
                ?: errorType("unresolved coalescing left")
            CfirBinaryOpKind.PIPELINE                 -> binaryOp.right.coneTypeOrNull
                ?: errorType("unresolved pipeline right")
        }
        binaryOp.replaceConeTypeOrNull(resultType)
        return binaryOp
    }

    override fun transformTypeOperator(
        typeOperator: CfirTypeOperator,
        data: ResolutionMode,
    ): CfirExpression {
        typeOperator.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val resultType = when (typeOperator.operation) {
            CfirTypeOperationKind.IS -> builtinTypes.boolType
            CfirTypeOperationKind.AS -> {
                val typeRef = typeOperator.typeRef
                if (typeRef is CfirResolvedTypeRef) typeRef.coneType
                else errorType("unresolved type in as-expression")
            }
        }
        typeOperator.replaceConeTypeOrNull(resultType)
        return typeOperator
    }

    // ── Error Expression ──────────────────────────────────────────────────────

    override fun transformErrorExpression(
        errorExpression: CfirErrorExpression,
        data: ResolutionMode,
    ): CfirExpression {
        errorExpression.replaceConeTypeOrNull(ConeErrorType(errorExpression.diagnostic))
        return errorExpression
    }

    // ── For-In / Loop ─────────────────────────────────────────────────────────

    override fun transformForInExpression(
        forInExpression: CfirForInExpression,
        data: ResolutionMode,
    ): CfirExpression {
        forInExpression.iterable.resolveIndependently()
        val iterVarType = inferIterableElementType(forInExpression.iterable.coneTypeOrNull)

        val varDecl = forInExpression.variable
        if (varDecl.returnTypeRef !is CfirResolvedTypeRef) {
            varDecl.replaceReturnTypeRef(
                varDecl.returnTypeRef.resolvedTypeFromPrototype(iterVarType, varDecl.returnTypeRef.source)
            )
        }

        forInExpression.replaceConeTypeOrNull(builtinTypes.unitType)
        return forInExpression
    }

    private fun inferIterableElementType(iterableType: ConeCangJieType?): ConeCangJieType {
        if (iterableType == null) return errorType("iterable has no type")
        when (iterableType) {
            is ConeClassLikeType -> {
                if (iterableType.classId == StdlibClassIds.Range) {
                    return iterableType.typeArguments.firstOrNull()?.type ?: ConePrimitiveType.INT64
                }
                val typeArgs = iterableType.typeArguments
                if (typeArgs.isNotEmpty()) return typeArgs.first().type
            }
            is ConeStructType -> {
                if (iterableType.classId == StdlibClassIds.Range) {
                    return iterableType.typeArguments.firstOrNull()?.type ?: ConePrimitiveType.INT64
                }
            }
            else -> Unit
        }
        return errorType("cannot infer element type from: $iterableType")
    }

    override fun transformLoopExpression(
        loopExpression: CfirLoopExpression,
        data: ResolutionMode,
    ): CfirExpression {
        loopExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        loopExpression.replaceConeTypeOrNull(builtinTypes.unitType)
        return loopExpression
    }

    // ── Try / Catch ───────────────────────────────────────────────────────────

    override fun transformTryExpression(
        tryExpression: CfirTryExpression,
        data: ResolutionMode,
    ): CfirExpression {
        tryExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val branchTypes = buildList {
            tryExpression.tryBlock.coneTypeOrNull?.let { add(it) }
            tryExpression.catches.forEach { it.body.coneTypeOrNull?.let { t -> add(t) } }
        }
        tryExpression.replaceConeTypeOrNull(
            when {
                branchTypes.isEmpty() -> builtinTypes.unitType
                branchTypes.size == 1 -> branchTypes.first()
                else                  -> commonSupertype(branchTypes)
            }
        )
        return tryExpression
    }

    // ── Subscript ─────────────────────────────────────────────────────────────

    override fun transformSubscriptExpression(
        subscriptExpression: CfirSubscriptExpression,
        data: ResolutionMode,
    ): CfirExpression {
        subscriptExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val resultType = when (val receiverType = subscriptExpression.receiver.coneTypeOrNull) {
            is ConeTupleType -> {
                val indexValue = extractConstantIntIndex(subscriptExpression.indices.firstOrNull())
                if (indexValue != null && indexValue in receiverType.elementTypes.indices) {
                    receiverType.elementTypes[indexValue]
                } else {
                    errorType("tuple index out of bounds or non-constant")
                }
            }
            is ConeVArrayType -> receiverType.elementType
            else -> {
                val arrayElementType = receiverType?.arrayElementType
                arrayElementType
                    ?: if (receiverType != null) {
                        resolveSubscriptExpressionType(subscriptExpression, receiverType, data)
                    } else {
                        errorType("receiver has no type")
                    }
            }
        }
        subscriptExpression.replaceConeTypeOrNull(resultType)
        return subscriptExpression
    }

    private fun resolveSubscriptExpressionType(
        subscriptExpression: CfirSubscriptExpression,
        receiverType: ConeCangJieType,
        data: ResolutionMode,
    ): ConeCangJieType {
        val argTypes = subscriptExpression.indices.mapNotNull { it.coneTypeOrNull }
        CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
            Name.identifier("[]"),
            receiverType,
            argTypes,
        )?.let { return it.returnType }

        val getCall = buildFunctionCall {
            source = subscriptExpression.source
            calleeReference = buildNamedReference {
                source = subscriptExpression.source
                name = OperatorNameConventions.GET
            }
            explicitReceiver = subscriptExpression.receiver
            argumentList = buildArgumentList {
                arguments.addAll(subscriptExpression.indices)
            }
            origin = CfirFunctionCallOrigin.Operator
        }

        val resolvedCall = callResolver.resolveCallAndSelectCandidate(getCall, data)
        (resolvedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            return ConeErrorType(diagnostic)
        }

        val completedCall = components.callCompleter.completeCall(resolvedCall, data)
        (completedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            return ConeErrorType(diagnostic)
        }

        return completedCall.coneTypeOrNull ?: errorType("no subscript operator for: $receiverType")
    }

    private fun resolveSubscriptSetAssignment(
        assignment: CfirAssignment,
        subscriptExpression: CfirSubscriptExpression,
        data: ResolutionMode,
    ) {
        val setCall = buildFunctionCall {
            source = subscriptExpression.source
            calleeReference = buildNamedReference {
                source = subscriptExpression.source
                name = OperatorNameConventions.SET
            }
            explicitReceiver = subscriptExpression.receiver
            argumentList = buildArgumentList {
                arguments.addAll(subscriptExpression.indices)
                arguments.add(assignment.rValue)
            }
            origin = CfirFunctionCallOrigin.Operator
        }

        val resolvedCall = callResolver.resolveCallAndSelectCandidate(setCall, data)
        (resolvedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            subscriptExpression.replaceConeTypeOrNull(ConeErrorType(diagnostic, delegatedType = builtinTypes.unitType))
            return
        }

        val completedCall = components.callCompleter.completeCall(resolvedCall, data)
        (completedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            subscriptExpression.replaceConeTypeOrNull(ConeErrorType(diagnostic, delegatedType = builtinTypes.unitType))
            return
        }

        subscriptExpression.replaceConeTypeOrNull(completedCall.coneTypeOrNull ?: builtinTypes.unitType)
    }

    private fun extractConstantIntIndex(expr: CfirExpression?): Int? {
        if (expr !is CfirLiteralExpression || expr.kind != CfirLiteralKind.INT) return null
        return (expr.value as? Long)?.toInt() ?: (expr.value as? Int)
    }

    // ── Lambda ────────────────────────────────────────────────────────────────

    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: CfirAnonymousFunctionExpression,
        data: ResolutionMode,
    ): CfirExpression {
        val anonFunc = anonymousFunctionExpression.anonymousFunction
        val expectedFuncType = data.expectedTypeOrNull as? ConeFuncType

        val hasUnresolvedParameterType = anonFunc.valueParameters.any { it.returnTypeRef !is CfirResolvedTypeRef }
        if (expectedFuncType == null && hasUnresolvedParameterType) {
            // Keep top-level lambda shape unresolved until call completion provides an expected function type.
            // Eagerly fixing returnType here turns lambda return mismatches into outer argument mismatches.
            return anonymousFunctionExpression
        }

        val parameterTypes = withNewLocalScope(scopeAction = { lambdaScope ->
            anonFunc.valueParameters.mapIndexed { i, param ->
                val expectedParamType = expectedFuncType?.parameterTypes?.getOrNull(i)
                val declaredParamType = (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                if (param.returnTypeRef !is CfirResolvedTypeRef && expectedParamType != null) {
                    param.replaceReturnTypeRef(
                        param.returnTypeRef.resolvedTypeFromPrototype(expectedParamType, param.returnTypeRef.source)
                    )
                }
                (param.symbol as? CfirCallableSymbol<*>)?.let { sym ->
                    lambdaScope.addVariable(param.name, sym)
                }
                declaredParamType ?: expectedParamType
            }
        }) { anonFunc.body?.resolveIndependently() }

        val returnType = when {
            anonFunc.returnTypeRef is CfirResolvedTypeRef -> (anonFunc.returnTypeRef as CfirResolvedTypeRef).coneType
            expectedFuncType != null -> expectedFuncType.returnType
            else -> anonFunc.body?.coneTypeOrNull
        }

        if (returnType != null && anonFunc.returnTypeRef !is CfirResolvedTypeRef) {
            anonFunc.replaceReturnTypeRef(
                returnType.toCfirResolvedTypeRef(anonFunc.returnTypeRef.source, anonFunc.returnTypeRef),
            )
        }

        if (returnType != null && parameterTypes.all { it != null }) {
            // CfirAnonymousFunctionExpression.coneTypeOrNull is derived from anonymousFunction.typeRef.
            // Keep the source of truth on declaration side instead of writing expression cone type directly.
            val lambdaType = ConeFuncType(parameterTypes.filterNotNull(), returnType)
            anonFunc.replaceTypeRef(lambdaType.toCfirResolvedTypeRef(anonFunc.typeRef.source, anonFunc.typeRef))
        }
        return anonymousFunctionExpression
    }

    // ── Range ─────────────────────────────────────────────────────────────────

    override fun transformRangeExpression(
        rangeExpression: CfirRangeExpression,
        data: ResolutionMode,
    ): CfirExpression {
        rangeExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val startType = rangeExpression.start.coneTypeOrNull
        val endType   = rangeExpression.end.coneTypeOrNull
        val elementType = when {
            startType != null && startType == endType -> IdealTypeResolver.resolveIfIdeal(startType, null)
            startType != null                         -> IdealTypeResolver.resolveIfIdeal(startType, null)
            else                                      -> ConePrimitiveType.INT64
        }
        rangeExpression.replaceConeTypeOrNull(
            constructNamedType(
                classId = StdlibClassIds.Range,
                typeArguments = listOf(ConeTypeProjection(elementType)),
            )
        )
        return rangeExpression
    }

    // ── Spawn ─────────────────────────────────────────────────────────────────

    override fun transformSpawnExpression(
        spawnExpression: CfirSpawnExpression,
        data: ResolutionMode,
    ): CfirExpression {
        spawnExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val taskReturnType = spawnExpression.body.coneTypeOrNull ?: builtinTypes.unitType
        spawnExpression.replaceConeTypeOrNull(
            constructNamedType(
                classId = StdlibClassIds.Future,
                typeArguments = listOf(ConeTypeProjection(taskReturnType)),
            )
        )
        return spawnExpression
    }

    private fun <T> completeResolvedAccess(
        access: T,
        data: ResolutionMode,
    ): T where T : CfirExpression, T : CfirResolvable {
        val candidateReference = access.calleeReference as? CfirNamedReferenceWithCandidate
        if (candidateReference != null) {
            return components.callCompleter.completeCall(access, data)
        }

        if (access.coneTypeOrNull == null) {
            when (access.calleeReference) {
                is CfirResolvedNamedReference,
                is CfirErrorNamedReference,
                -> access.replaceConeTypeOrNull(components.typeFromCallee(access))
                else -> Unit
            }
        }
        return access
    }

    // ── Stdlib / ClassId Helpers ──────────────────────────────────────────────

    private fun stdlibStringType(): ConeCangJieType {
        val symbol = components.symbolProvider.getClassLikeSymbolByClassId(StdlibClassIds.String)
        if (symbol != null) {
            return constructClassLikeType(symbol, StdlibClassIds.String, emptyList())
        }
        return ConeClassLikeType(StdlibClassIds.String.toLookupTag())
    }

    private fun constructNamedType(
        classId: ClassId,
        typeArguments: List<ConeTypeProjection> = emptyList(),
    ): ConeCangJieType {
        val symbol = components.symbolProvider.getClassLikeSymbolByClassId(classId)
        return if (symbol != null) constructClassLikeType(symbol, classId, typeArguments)
        else ConeClassLikeType(classId.toLookupTag(), typeArguments)
    }

    private fun constructClassLikeType(
        symbol: CfirClassLikeSymbol<*>,
        classId: ClassId,
        typeArguments: List<ConeTypeProjection>,
    ): ConeCangJieType = when (symbol) {
        is CfirTypeAliasSymbol -> ConeTypeAliasType(classId, typeArguments = typeArguments)

        is CfirPrimitiveTypeSymbol -> ConePrimitiveType(symbol.kind)
        is CfirInterfaceSymbol -> ConeClassLikeType(classId.toLookupTag(), typeArguments, isInterface = true)
        is CfirStructSymbol -> ConeStructType(classId.toLookupTag(), typeArguments)
        is CfirEnumSymbol -> ConeEnumType(classId.toLookupTag(), typeArguments, isRefEnum = symbol.isRefEnum)
        else -> ConeClassLikeType(classId.toLookupTag(), typeArguments)
    }

    // ── Common Supertype ──────────────────────────────────────────────────────

    private fun commonSupertype(types: List<ConeCangJieType>): ConeCangJieType {
        if (types.isEmpty()) return builtinTypes.unitType
        val first = types.first()
        if (types.all { it == first }) return first

        val nonNothing = types.filter { it != ConePrimitiveType.NOTHING }
        if (nonNothing.isEmpty()) return ConePrimitiveType.NOTHING
        if (nonNothing.size == 1) return nonNothing.first()

        val typeCheckerContext = session.typeContext
        val chains = nonNothing.map { collectSupertypeChain(it, typeCheckerContext) }

        val commonTypes = chains.reduce { acc, chain ->
            acc.filter { candidate ->
                chain.any { typeCheckerContext.isSameTypeConstructor(candidate, it) }
            }
        }

        if (commonTypes.isEmpty()) return ConeUnionType(types.toSet())

        return commonTypes.firstOrNull { candidate ->
            commonTypes.none { other ->
                !typeCheckerContext.isSameTypeConstructor(candidate, other) &&
                        collectSupertypeChain(other, typeCheckerContext).any {
                            typeCheckerContext.isSameTypeConstructor(it, candidate)
                        }
            }
        } ?: commonTypes.first()
    }

    private fun collectSupertypeChain(
        type: ConeCangJieType,
        context: ConeInferenceContext,
    ): List<ConeCangJieType> {
        val result = mutableListOf<ConeCangJieType>()
        val visited = mutableSetOf<ConeCangJieType>()
        val queue = ArrayDeque<ConeCangJieType>()
        queue.add(type)
        visited.add(type)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result += current
            val constructor = with(context) { (current as? ConeRigidType)?.typeConstructor() } ?: continue
            val supertypes = with(context) {
                constructor.supertypes().mapNotNull { it as? ConeCangJieType }
            }
            supertypes.forEach { supertype ->
                if (visited.add(supertype)) queue.add(supertype)
            }
        }
        return result
    }

    // ── Scope Utilities ───────────────────────────────────────────────────────

    /**
     * Execute [block] inside a fresh local scope, restoring the outer scope on exit.
     * The overload that takes [scopeAction] provides the scope object to [scopeAction]
     * before running [block], so that the scope can be populated (e.g. lambda params).
     */
    private inline fun <T> withNewLocalScope(crossinline block: () -> T): T {
        val saved = context.towerDataContext
        val scope = CfirLocalScopeImpl()
        context.addLocalScope(scope)
        return try { block() } finally { context.replaceTowerDataContext(saved) }
    }

    private inline fun <T> withNewLocalScope(
        crossinline scopeAction: (CfirLocalScopeImpl) -> T,
        crossinline block: () -> Unit,
    ): T {
        val saved = context.towerDataContext
        val scope = CfirLocalScopeImpl()
        context.addLocalScope(scope)
        return try {
            val result = scopeAction(scope)
            block()
            result
        } finally {
            context.replaceTowerDataContext(saved)
        }
    }

    /**
     * `super` 的语义在仓颉里是固定的：
     * 1. 只能出现在 class 内部；
     * 2. 绑定到当前 class 声明的直接父 class；
     * 3. 不能落到接口父类型，也不能从继承链上做兜底推断。
     *
     * 这里在进入 tower resolve 前就把接收者类型确定下来，
     * 避免后续 `ExpressionReceiverValue.scope()` 再遇到未解析的 `super`。
     */
    private fun resolveImplicitSuperReceiverType(owner: CfirClass): ConeCangJieType {
        val directClassSuperTypes = owner.directClassSuperTypes()
        return when (directClassSuperTypes.size) {
            1 -> directClassSuperTypes.single()
            0 -> errorType("`super` requires a direct class supertype in ${owner.name}")
            else -> errorType("`super` is ambiguous because ${owner.name} declares multiple direct class supertypes")
        }
    }

    /**
     * 预留给未来显式 `super<T>` / `super<Base>` 语法：
     * 即使语法层已经指定了目标类型，也必须严格受“当前 class 的直接父 class”约束。
     */
    private fun resolveExplicitSuperReceiverType(
        owner: CfirClass,
        resolvedSuperTypeRef: CfirResolvedTypeRef,
    ): ConeCangJieType {
        val requestedType = resolvedSuperTypeRef.coneType
        if (!requestedType.isDirectClassSuperType()) {
            return errorType("`super` can only target a direct class supertype of ${owner.name}")
        }

        val directClassSuperTypes = owner.directClassSuperTypes()
        if (directClassSuperTypes.none { it == requestedType }) {
            return errorType("`super` can only target a direct class supertype of ${owner.name}")
        }

        return requestedType
    }

    private fun CfirClass.directClassSuperTypes(): List<ConeCangJieType> {
        return superTypeRefs
            .filterIsInstance<CfirResolvedTypeRef>()
            .map(CfirResolvedTypeRef::coneType)
            .filter { candidate -> candidate.isDirectClassSuperType() }
    }

    private fun ConeCangJieType.isDirectClassSuperType(): Boolean = when (this) {
        is ConeClassLikeType -> !isInterface
        is ConeStructType, is ConeEnumType -> true
        else -> false
    }

    // ── Small Extension Utilities ─────────────────────────────────────────────

    private fun CfirExpression.resolveIndependently() {
        transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
    }

    private fun CfirExpression.resolveIndependently(body: CfirBlock?) {
        body?.transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
    }

    private val ResolutionMode.expectedTypeOrNull: ConeCangJieType?
        get() = (this as? ResolutionMode.WithExpectedType)?.expectedTypeRef?.coneType

    private fun <T : CfirQualifiedAccessExpression> resolveAccessTypeArguments(access: T): T {
        if (access.typeArguments.isEmpty()) return access

        val additionalTypeParameters = context.containers
            .asSequence()
            .filterIsInstance<CfirDeclaration>()
            .flatMap { extractTypeParameters(it).asSequence() }
            .toList()

        val config = CfirTypeResolutionConfiguration(
            useSiteFile = context.file,
            topContainer = context.containers.lastOrNull(),
        ).withAdditionalTypeParameters(additionalTypeParameters)

        val resolvedTypeArguments = access.typeArguments.map { typeRef ->
            when (typeRef) {
                is CfirResolvedTypeRef -> typeRef
                is CfirImplicitTypeRef -> typeRef
                else -> specificTypeResolverTransformer.transformTypeRef(typeRef, config)
            }
        }

        access.replaceTypeArguments(resolvedTypeArguments)
        return access
    }

    private fun resolveSuperTypeRef(typeRef: CfirTypeRef): CfirTypeRef {
        if (typeRef is CfirResolvedTypeRef || typeRef is CfirImplicitTypeRef) return typeRef

        val additionalTypeParameters = context.containers
            .asSequence()
            .filterIsInstance<CfirDeclaration>()
            .flatMap { extractTypeParameters(it).asSequence() }
            .toList()

        val config = CfirTypeResolutionConfiguration(
            useSiteFile = context.file,
            topContainer = context.containers.lastOrNull(),
        ).withAdditionalTypeParameters(additionalTypeParameters)

        return specificTypeResolverTransformer.transformTypeRef(typeRef, config)
    }

    private fun extractTypeParameters(declaration: CfirDeclaration): List<CfirTypeParameter> = when (declaration) {
        is CfirClass -> declaration.typeParameters
        is CfirInterface -> declaration.typeParameters
        is CfirStruct -> declaration.typeParameters
        is CfirEnum -> declaration.typeParameters
        is CfirFunction -> declaration.typeParameters
        is CfirConstructor -> declaration.typeParameters
        is CfirProperty -> declaration.typeParameters
        is CfirFieldVariable -> declaration.typeParameters
        is CfirValueParameter -> declaration.typeParameters
        is CfirExtend -> declaration.typeParameters
        is CfirTypeAlias -> declaration.typeParameters
        is CfirPatternVariable -> declaration.typeParameters
        is CfirMacroDeclaration -> declaration.typeParameters
        is CfirMainFunction -> declaration.typeParameters
        is CfirFinalizer -> declaration.typeParameters
        is CfirEnumConstructor -> declaration.typeParameters
        else -> emptyList()
    }
}
