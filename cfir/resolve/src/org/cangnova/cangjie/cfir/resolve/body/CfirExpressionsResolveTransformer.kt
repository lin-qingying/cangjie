package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildQualifiedAccess
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedNamedReferenceImpl
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.resolve.CfirTypeCheckerContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name

/**
 * Expression resolve transformer.
 *
 * Responsibility: compute and propagate expression types only.
 * This includes literals, accesses, calls, patterns, control-flow, and lambdas.
 *
 * Diagnostic reporting is intentionally NOT performed here. When resolution
 * yields a [CfirCallResolutionResult.ResolvedWithErrors] candidate, the
 * candidate's diagnostics remain attached to the resolved node and are
 * forwarded by a dedicated checker pass (e.g. CfirCallsCheckerTransformer)
 * after the full resolve phase completes.
 */
@OptIn(CfirImplementationDetail::class)
class CfirExpressionsResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
) : CfirPartialBodyResolveTransformer(transformer) {

    private val builtinTypes get() = session.builtinTypes
    private val callResolver get() = components.callResolver
    private val towerResolver get() = components.towerResolver

    // ── Literals ─────────────────────────────────────────────────────────────

    override fun transformLiteralExpression(
        literalExpression: CfirLiteralExpression,
        data: CfirResolutionMode,
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

    // ── Property Access ───────────────────────────────────────────────────────

    override fun transformPropertyAccess(
        propertyAccess: CfirPropertyAccess,
        data: CfirResolutionMode,
    ): CfirExpression {
        propertyAccess.explicitReceiver?.resolveIndependently()

        val reference = propertyAccess.calleeReference

        if (reference is CfirResolvedNamedReference) {
            if (data.expectedTypeOrNull == null) {
                val current = propertyAccess.coneTypeOrNull
                if (current != null && current !is ConeErrorType) return propertyAccess
            }
            propertyAccess.replaceConeTypeOrNull(
                extractTypeFromSymbolWithExpected(reference.resolvedSymbol, data)
            )
            return propertyAccess
        }

        if (reference !is CfirNamedReference) {
            propertyAccess.replaceConeTypeOrNull(ConeErrorType("non-name reference"))
            return propertyAccess
        }

        return if (propertyAccess.explicitReceiver != null) {
            propertyAccess.resolveWithReceiverInPlace(reference.name, propertyAccess.explicitReceiver!!, data)
        } else {
            resolvePropertyAccessWithoutReceiver(propertyAccess, reference, data)
        }
    }

    private fun resolvePropertyAccessWithoutReceiver(
        propertyAccess: CfirPropertyAccess,
        reference: CfirNamedReference,
        data: CfirResolutionMode,
    ): CfirExpression {
        val name = reference.name
        val expectedType = data.expectedTypeOrNull

        // 1. Try callable access via phase-3 resolution.
        val callables = towerResolver.findCallables(name)
        if (callables.isNotEmpty()) {
            val ctx = components.createResolutionContext(expectedType)
            if (ctx != null) {
                val qualifiedAccess = propertyAccess.toCallableQualifiedAccess()
                applyCallableAccessResult(
                    access = qualifiedAccess,
                    reference = reference,
                    result = resolveCallableAccessWithoutReceiver(qualifiedAccess, reference, emptyList(), ctx),
                    expectedType = expectedType,
                    resolutionContext = ctx,
                )
                return qualifiedAccess
            }
        }

        // 2. Try local / tower variables.
        towerResolver.findVariables(name).firstOrNull()?.let { symbol ->
            propertyAccess.bindResolvedReference(name, symbol)
            propertyAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(symbol, data))
            return propertyAccess
        }

        // 3. Try classifiers (type aliases, classes, etc.).
        towerResolver.findClassifiers(name).firstOrNull()?.let { classifier ->
            propertyAccess.bindResolvedReference(name, classifier)
            propertyAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(classifier, data))
            return propertyAccess
        }

        // 4. Callable symbol fallback (function reference without call).
        if (callables.isNotEmpty()) {
            val symbol = callables.first()
            propertyAccess.bindResolvedReference(name, symbol)
            propertyAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(symbol, data))
        } else {
            propertyAccess.replaceConeTypeOrNull(ConeErrorType(ConeUnresolvedReferenceError(name)))
        }
        return propertyAccess
    }

    // ── Qualified Access ──────────────────────────────────────────────────────

    override fun transformQualifiedAccess(
        qualifiedAccess: CfirQualifiedAccess,
        data: CfirResolutionMode,
    ): CfirExpression {
        qualifiedAccess.explicitReceiver?.resolveIndependently()

        val reference = qualifiedAccess.calleeReference

        if (reference is CfirResolvedNamedReference) {
            if (data.expectedTypeOrNull == null) {
                val current = qualifiedAccess.coneTypeOrNull
                if (current != null && current !is ConeErrorType) return qualifiedAccess
            }
            qualifiedAccess.replaceConeTypeOrNull(
                extractTypeFromSymbolWithExpected(reference.resolvedSymbol, data)
            )
            return qualifiedAccess
        }

        if (reference !is CfirNamedReference) {
            qualifiedAccess.replaceConeTypeOrNull(ConeErrorType("non-name reference"))
            return qualifiedAccess
        }

        return if (qualifiedAccess.explicitReceiver != null) {
            qualifiedAccess.resolveWithReceiverInPlace(reference.name, qualifiedAccess.explicitReceiver!!, data)
        } else {
            resolveQualifiedAccessWithoutReceiver(qualifiedAccess, reference, data)
        }
    }

    private fun resolveQualifiedAccessWithoutReceiver(
        qualifiedAccess: CfirQualifiedAccess,
        reference: CfirNamedReference,
        data: CfirResolutionMode,
    ): CfirExpression {
        val name = reference.name
        val expectedType = data.expectedTypeOrNull

        towerResolver.findVariables(name).firstOrNull()?.let { symbol ->
            qualifiedAccess.bindResolvedReference(name, symbol)
            qualifiedAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(symbol, data))
            return qualifiedAccess
        }

        towerResolver.findClassifiers(name).firstOrNull()?.let { classifier ->
            qualifiedAccess.bindResolvedReference(name, classifier)
            qualifiedAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(classifier, data))
            return qualifiedAccess
        }

        val callables = towerResolver.findCallables(name)
        if (callables.isNotEmpty()) {
            val ctx = components.createResolutionContext(expectedType)
            if (ctx != null) {
                applyCallableAccessResult(
                    access = qualifiedAccess,
                    reference = reference,
                    result = resolveCallableAccessWithoutReceiver(
                        qualifiedAccess, reference, qualifiedAccess.typeArguments, ctx
                    ),
                    expectedType = expectedType,
                    resolutionContext = ctx,
                )
                return qualifiedAccess
            }
            val symbol = callables.first()
            qualifiedAccess.bindResolvedReference(name, symbol)
            qualifiedAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(symbol, data))
        } else {
            qualifiedAccess.replaceConeTypeOrNull(ConeErrorType(ConeUnresolvedReferenceError(name)))
        }
        return qualifiedAccess
    }

    // ── Function Call ─────────────────────────────────────────────────────────

    override fun transformFunctionCall(
        functionCall: CfirFunctionCall,
        data: CfirResolutionMode,
    ): CfirExpression {
        functionCall.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        val reference = functionCall.calleeReference

        if (reference is CfirResolvedNamedReference) {
            val appliedReturnType = (reference as? CfirResolvedAppliedCallableReference)?.substitutedReturnType
            functionCall.replaceConeTypeOrNull(
                appliedReturnType ?: extractReturnTypeFromSymbol(reference.resolvedSymbol)
            )
            return functionCall
        }

        if (reference !is CfirNamedReference) {
            functionCall.replaceConeTypeOrNull(ConeErrorType("non-name callee reference"))
            return functionCall
        }

        val expectedType = data.expectedTypeOrNull
        val ctx = components.createResolutionContext(expectedType)
            ?: return resolveCallLegacy(functionCall, reference)

        return resolveCallPhase3(functionCall, reference, ctx)
    }

    private fun resolveCallPhase3(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
        ctx: CfirResolutionContext,
    ): CfirExpression {
        val callInfo = buildCallInfo(
            callSite = functionCall,
            kind = CfirCallKind.Function,
            name = reference.name,
            explicitReceiver = functionCall.explicitReceiver,
            arguments = functionCall.arguments,
            typeArguments = functionCall.typeArguments,
        )

        when (val result = callResolver.resolveCallAndSelectCandidate(callInfo, ctx)) {
            is CfirCallResolutionResult.Success -> {
                applySuccessfulCallResult(functionCall, reference.name, result.candidate, ctx.expectedType)
            }
            is CfirCallResolutionResult.ResolvedWithErrors -> {
                // Diagnostics remain on the candidate; checker pass will report them.
                applySuccessfulCallResult(functionCall, reference.name, result.candidate, ctx.expectedType)
            }
            is CfirCallResolutionResult.Ambiguity -> {
                functionCall.replaceConeTypeOrNull(ConeErrorType("ambiguous call: ${reference.name}"))
            }
            is CfirCallResolutionResult.NoCandidate -> {
                resolveCallFallbacks(functionCall, reference, ctx)
            }
            is CfirCallResolutionResult.LegacySuccess -> {
                functionCall.bindResolvedReference(reference.name, result.symbol)
                functionCall.replaceConeTypeOrNull(result.returnType)
            }
            is CfirCallResolutionResult.LegacyAmbiguity -> {
                functionCall.replaceConeTypeOrNull(ConeErrorType("ambiguous call: ${reference.name}"))
            }
        }
        return functionCall
    }

    /** Apply a resolved (possibly-with-errors) candidate to a function call node. */
    private fun applySuccessfulCallResult(
        functionCall: CfirFunctionCall,
        name: Name,
        candidate: CfirCandidate,
        expectedType: ConeCangJieType?,
    ) {
        retransformLambdaArguments(functionCall, candidate)
        functionCall.replaceCalleeReference(buildAppliedCallableReference(name, candidate, components))
        val returnType = components.callCompleter.completedResultType(candidate)
            ?: ConeErrorType("unresolved return type")
        functionCall.replaceConeTypeOrNull(refineResolvedCallType(returnType, expectedType))
    }

    /**
     * Ordered fallback chain for unresolved function calls:
     * 1. Classifier qualifier (e.g. `SomeType()` without constructor arguments)
     * 2. Enum constructor
     * 3. First-class callable variable invoke
     * 4. Builtin operator
     */
    private fun resolveCallFallbacks(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
        ctx: CfirResolutionContext,
    ) {
        if (tryClassifierQualifierFallback(functionCall, reference)) return
        if (tryEnumConstructorFallback(functionCall, reference, ctx)) return
        if (tryCallableVariableInvokeFallback(functionCall, reference)) return

        val builtinType = tryBuiltinOperatorFallback(functionCall, reference)
        functionCall.replaceConeTypeOrNull(
            builtinType ?: ConeErrorType(ConeUnresolvedNameError(reference.name))
        )
    }

    // ── Callable Access (without call) ────────────────────────────────────────

    private fun resolveCallableAccessWithoutReceiver(
        access: CfirExpression,
        reference: CfirNamedReference,
        typeArguments: List<CfirTypeRef>,
        ctx: CfirResolutionContext,
    ): CfirCallResolutionResult {
        val callInfo = buildCallInfo(
            callSite = access,
            kind = CfirCallKind.VariableAccess,
            name = reference.name,
            explicitReceiver = null,
            arguments = emptyList(),
            typeArguments = typeArguments,
        )
        return callResolver.resolveCallAndSelectCandidate(callInfo, ctx)
    }

    private fun <T> applyCallableAccessResult(
        access: T,
        reference: CfirNamedReference,
        result: CfirCallResolutionResult,
        expectedType: ConeCangJieType?,
        resolutionContext: CfirResolutionContext? = null,
    ) where T : CfirExpression, T : org.cangnova.cangjie.cfir.CfirResolvable {
        when (result) {
            is CfirCallResolutionResult.Success -> {
                applyResolvedCallableAccessCandidate(access, reference, result.candidate, expectedType)
            }
            is CfirCallResolutionResult.ResolvedWithErrors -> {
                // Diagnostics remain on the candidate; checker pass will report them.
                applyResolvedCallableAccessCandidate(access, reference, result.candidate, expectedType)
            }
            is CfirCallResolutionResult.Ambiguity -> {
                access.replaceConeTypeOrNull(ConeErrorType("ambiguous call: ${reference.name}"))
            }
            is CfirCallResolutionResult.NoCandidate -> {
                if (resolutionContext != null) {
                    val enumCandidate = resolveEnumConstructorAccess(access, reference, resolutionContext)
                    if (enumCandidate != null) {
                        applyResolvedCallableAccessCandidate(access, reference, enumCandidate, expectedType)
                        return
                    }
                }
                access.replaceConeTypeOrNull(ConeErrorType(ConeUnresolvedReferenceError(reference.name)))
            }
            is CfirCallResolutionResult.LegacySuccess -> {
                access.replaceConeTypeOrNull(result.returnType)
            }
            is CfirCallResolutionResult.LegacyAmbiguity -> {
                access.replaceConeTypeOrNull(ConeErrorType("ambiguous call: ${reference.name}"))
            }
        }
    }

    private fun <T> applyResolvedCallableAccessCandidate(
        access: T,
        reference: CfirNamedReference,
        candidate: CfirCandidate,
        expectedType: ConeCangJieType?,
    ) where T : CfirExpression, T : org.cangnova.cangjie.cfir.CfirResolvable {
        access.replaceCalleeReference(buildAppliedCallableReference(reference.name, candidate, components))
        val returnType = components.callCompleter.completedResultType(candidate)
            ?: ConeErrorType("unresolved callable access type")
        access.replaceConeTypeOrNull(refineResolvedCallType(returnType, expectedType))
    }

    // ── Legacy Call Resolution ────────────────────────────────────────────────

    private fun resolveCallLegacy(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
    ): CfirExpression {
        when (val result = callResolver.resolveCall(reference.name, functionCall.arguments)) {
            is CfirCallResolutionResult.LegacySuccess -> {
                functionCall.bindResolvedReference(reference.name, result.symbol)
                functionCall.replaceConeTypeOrNull(result.returnType)
            }
            is CfirCallResolutionResult.LegacyAmbiguity -> {
                functionCall.replaceConeTypeOrNull(ConeErrorType("ambiguous call: ${reference.name}"))
            }
            is CfirCallResolutionResult.NoCandidate -> {
                if (!tryClassifierQualifierFallback(functionCall, reference) &&
                    !tryCallableVariableInvokeFallback(functionCall, reference)
                ) {
                    functionCall.replaceConeTypeOrNull(ConeErrorType(ConeUnresolvedNameError(reference.name)))
                }
            }
            else -> functionCall.replaceConeTypeOrNull(ConeErrorType("unexpected resolution result"))
        }
        return functionCall
    }

    // ── Block ─────────────────────────────────────────────────────────────────

    override fun transformBlock(block: CfirBlock, data: CfirResolutionMode): CfirExpression {
        block.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
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
        data: CfirResolutionMode,
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
            branch.transformPattern(transformer, CfirResolutionMode.ContextIndependent)
            resolvePattern(branch.pattern, subjectType)

            branch.transformGuard(transformer, CfirResolutionMode.ContextIndependent)
            branch.transformBody(transformer, CfirResolutionMode.ContextIndependent)

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
            this.returnTypeRef = buildResolvedTypeRef { coneType = type }
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
        data: CfirResolutionMode,
    ): CfirExpression {
        ifExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        val thenType = ifExpression.thenBranch.coneTypeOrNull
        val elseType = ifExpression.elseBranch?.coneTypeOrNull
        val resultType = when {
            thenType == null        -> elseType ?: builtinTypes.unitType
            elseType == null        -> builtinTypes.unitType
            thenType == elseType    -> thenType
            else                    -> ConeUnionType(setOf(thenType, elseType))
        }
        ifExpression.replaceConeTypeOrNull(resultType)
        return ifExpression
    }

    // ── Return / Throw ────────────────────────────────────────────────────────

    override fun transformReturnExpression(
        returnExpression: CfirReturnExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        returnExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        returnExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        return returnExpression
    }

    override fun transformThrowExpression(
        throwExpression: CfirThrowExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        throwExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        throwExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        return throwExpression
    }

    // ── Assignment ────────────────────────────────────────────────────────────

    override fun transformAssignment(
        assignment: CfirAssignment,
        data: CfirResolutionMode,
    ): CfirExpression {
        assignment.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        assignment.replaceConeTypeOrNull(builtinTypes.unitType)
        return assignment
    }

    // ── Tuple / Array / String Literals ──────────────────────────────────────

    override fun transformTupleLiteral(
        tupleLiteral: CfirTupleLiteral,
        data: CfirResolutionMode,
    ): CfirExpression {
        tupleLiteral.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        val elementTypes = tupleLiteral.elements.map {
            it.coneTypeOrNull ?: ConeErrorType("unresolved element")
        }
        tupleLiteral.replaceConeTypeOrNull(ConeTupleType(elementTypes))
        return tupleLiteral
    }

    override fun transformArrayLiteral(
        arrayLiteral: CfirArrayLiteral,
        data: CfirResolutionMode,
    ): CfirExpression {
        arrayLiteral.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        val elementType = arrayLiteral.elements.firstNotNullOfOrNull { it.coneTypeOrNull }
            ?: ConeErrorType("empty array literal")
        arrayLiteral.replaceConeTypeOrNull(ConeArrayType(elementType))
        return arrayLiteral
    }

    override fun transformStringInterpolation(
        stringInterpolation: CfirStringInterpolation,
        data: CfirResolutionMode,
    ): CfirExpression {
        stringInterpolation.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        stringInterpolation.replaceConeTypeOrNull(stdlibStringType())
        return stringInterpolation
    }

    // ── Comparison / Binary / Type Operators ──────────────────────────────────

    override fun transformComparisonExpression(
        comparisonExpression: CfirComparisonExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        comparisonExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        val leftType  = comparisonExpression.left.coneTypeOrNull
        val rightType = comparisonExpression.right.coneTypeOrNull
        val resultType = if (leftType != null && rightType != null) {
            CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
                Name.identifier(comparisonExpression.operation.toFunctionName()),
                leftType,
                listOf(rightType),
            ) ?: builtinTypes.boolType
        } else {
            builtinTypes.boolType
        }
        comparisonExpression.replaceConeTypeOrNull(resultType)
        return comparisonExpression
    }

    override fun transformBinaryOp(
        binaryOp: CfirBinaryOp,
        data: CfirResolutionMode,
    ): CfirExpression {
        binaryOp.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        val resultType = when (binaryOp.kind) {
            CfirBinaryOpKind.AND, CfirBinaryOpKind.OR -> builtinTypes.boolType
            CfirBinaryOpKind.COALESCING               -> binaryOp.left.coneTypeOrNull
                ?: ConeErrorType("unresolved coalescing left")
            CfirBinaryOpKind.PIPELINE                 -> binaryOp.right.coneTypeOrNull
                ?: ConeErrorType("unresolved pipeline right")
        }
        binaryOp.replaceConeTypeOrNull(resultType)
        return binaryOp
    }

    override fun transformTypeOperator(
        typeOperator: CfirTypeOperator,
        data: CfirResolutionMode,
    ): CfirExpression {
        typeOperator.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        val resultType = when (typeOperator.operation) {
            CfirTypeOperationKind.IS -> builtinTypes.boolType
            CfirTypeOperationKind.AS -> {
                val typeRef = typeOperator.typeRef
                if (typeRef is CfirResolvedTypeRef) typeRef.coneType
                else ConeErrorType("unresolved type in as-expression")
            }
        }
        typeOperator.replaceConeTypeOrNull(resultType)
        return typeOperator
    }

    // ── Error Expression ──────────────────────────────────────────────────────

    override fun transformErrorExpression(
        errorExpression: CfirErrorExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        errorExpression.replaceConeTypeOrNull(ConeErrorType(errorExpression.reason))
        return errorExpression
    }

    // ── For-In / Loop ─────────────────────────────────────────────────────────

    override fun transformForInExpression(
        forInExpression: CfirForInExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        forInExpression.iterable.resolveIndependently()
        val iterVarType = inferIterableElementType(forInExpression.iterable.coneTypeOrNull)

        val varDecl = forInExpression.variable
        if (varDecl.returnTypeRef !is CfirResolvedTypeRef) {
            varDecl.replaceReturnTypeRef(buildResolvedTypeRef {
                source = varDecl.returnTypeRef.source
                delegatedTypeRef = varDecl.returnTypeRef
                coneType = iterVarType
            })
        }

        forInExpression.replaceConeTypeOrNull(builtinTypes.unitType)
        return forInExpression
    }

    private fun inferIterableElementType(iterableType: ConeCangJieType?): ConeCangJieType {
        if (iterableType == null) return ConeErrorType("iterable has no type")
        when (iterableType) {
            is ConeClassLikeType -> {
                if (iterableType.classId == StdlibClassIds.Range) {
                    return iterableType.typeArguments.firstOrNull() ?: ConePrimitiveType.INT64
                }
                val typeArgs = iterableType.typeArguments
                if (typeArgs.isNotEmpty()) return typeArgs.first()
            }
            is ConeStructType -> {
                if (iterableType.classId == StdlibClassIds.Range) {
                    return iterableType.typeArguments.firstOrNull() ?: ConePrimitiveType.INT64
                }
            }
            else -> Unit
        }
        return ConeErrorType("cannot infer element type from: $iterableType")
    }

    override fun transformLoopExpression(
        loopExpression: CfirLoopExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        loopExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        loopExpression.replaceConeTypeOrNull(builtinTypes.unitType)
        return loopExpression
    }

    // ── Try / Catch ───────────────────────────────────────────────────────────

    override fun transformTryExpression(
        tryExpression: CfirTryExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        tryExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
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
        data: CfirResolutionMode,
    ): CfirExpression {
        subscriptExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        val resultType = when (val receiverType = subscriptExpression.receiver.coneTypeOrNull) {
            is ConeTupleType -> {
                val indexValue = extractConstantIntIndex(subscriptExpression.indices.firstOrNull())
                if (indexValue != null && indexValue in receiverType.elementTypes.indices) {
                    receiverType.elementTypes[indexValue]
                } else {
                    ConeErrorType("tuple index out of bounds or non-constant")
                }
            }
            is ConeVArrayType -> receiverType.elementType
            is ConeArrayType  -> receiverType.elementType
            else -> {
                if (receiverType != null) {
                    val argTypes = subscriptExpression.indices.mapNotNull { it.coneTypeOrNull }
                    CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
                        Name.identifier("[]"), receiverType, argTypes
                    ) ?: ConeErrorType("no subscript operator for: $receiverType")
                } else {
                    ConeErrorType("receiver has no type")
                }
            }
        }
        subscriptExpression.replaceConeTypeOrNull(resultType)
        return subscriptExpression
    }

    private fun extractConstantIntIndex(expr: CfirExpression?): Int? {
        if (expr !is CfirLiteralExpression || expr.kind != CfirLiteralKind.INT) return null
        return (expr.value as? Long)?.toInt() ?: (expr.value as? Int)
    }

    // ── Lambda ────────────────────────────────────────────────────────────────

    override fun transformLambdaExpression(
        lambdaExpression: CfirLambdaExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        val anonFunc = lambdaExpression.anonymousFunction
        val expectedFuncType = data.expectedTypeOrNull as? ConeFuncType

        val paramTypes = withNewLocalScope(scopeAction = { lambdaScope ->
            anonFunc.valueParameters.mapIndexed { i, param ->
                val expectedParamType = expectedFuncType?.parameterTypes?.getOrNull(i)
                if (param.returnTypeRef !is CfirResolvedTypeRef && expectedParamType != null) {
                    param.replaceReturnTypeRef(buildResolvedTypeRef {
                        source = param.returnTypeRef.source
                        delegatedTypeRef = param.returnTypeRef
                        coneType = expectedParamType
                    })
                }
                (param.symbol as? CfirCallableSymbol<*>)?.let { sym ->
                    lambdaScope.addVariable(param.name, sym)
                }
                (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                    ?: expectedParamType
                    ?: ConeErrorType("cannot infer lambda param type at $i")
            }
        }) { anonFunc.body?.resolveIndependently() }

        val returnType = when {
            expectedFuncType != null                          -> expectedFuncType.returnType
            anonFunc.returnTypeRef is CfirResolvedTypeRef     -> (anonFunc.returnTypeRef as CfirResolvedTypeRef).coneType
            else                                              -> anonFunc.body?.coneTypeOrNull
                ?: ConeErrorType("cannot infer lambda return type")
        }

        lambdaExpression.replaceConeTypeOrNull(ConeFuncType(paramTypes, returnType))
        return lambdaExpression
    }

    // ── Range ─────────────────────────────────────────────────────────────────

    override fun transformRangeExpression(
        rangeExpression: CfirRangeExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        rangeExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        val startType = rangeExpression.start.coneTypeOrNull
        val endType   = rangeExpression.end.coneTypeOrNull
        val elementType = when {
            startType != null && startType == endType -> IdealTypeResolver.resolveIfIdeal(startType, null)
            startType != null                         -> IdealTypeResolver.resolveIfIdeal(startType, null)
            else                                      -> ConePrimitiveType.INT64
        }
        rangeExpression.replaceConeTypeOrNull(
            ConeStructType(ConeClassLookupTagImpl(StdlibClassIds.Range), listOf(elementType))
        )
        return rangeExpression
    }

    // ── Spawn ─────────────────────────────────────────────────────────────────

    override fun transformSpawnExpression(
        spawnExpression: CfirSpawnExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        spawnExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        val taskReturnType = spawnExpression.body.coneTypeOrNull ?: builtinTypes.unitType
        spawnExpression.replaceConeTypeOrNull(
            ConeClassLikeType(ConeClassLookupTagImpl(StdlibClassIds.Future), listOf(taskReturnType))
        )
        return spawnExpression
    }

    // ── Fallback Helpers ──────────────────────────────────────────────────────

    private fun tryClassifierQualifierFallback(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
    ): Boolean {
        if (functionCall.explicitReceiver != null || functionCall.arguments.isNotEmpty()) return false
        val classifier = towerResolver.findClassifiers(reference.name).firstOrNull() ?: return false
        val functionCallImpl = functionCall as? org.cangnova.cangjie.cfir.expressions.impl.CfirFunctionCallImpl
            ?: return false

        functionCallImpl.calleeReference = CfirResolvedNamedReferenceImpl(
            source = null, name = reference.name, resolvedSymbol = classifier
        )
        functionCall.replaceConeTypeOrNull(
            buildClassifierQualifierType(classifier, functionCall.typeArguments)
        )
        return true
    }

    private fun buildClassifierQualifierType(
        classifier: CfirClassSymbol,
        typeArguments: List<CfirTypeRef>,
    ): ConeCangJieType {
        val classId = resolveClassIdBySymbol(classifier)
            ?: return ConeErrorType("unresolved class id for symbol: ${classifier.cfir.name}")
        val resolvedTypeArgs = typeArguments.mapNotNull { (it as? CfirResolvedTypeRef)?.coneType }
        val fallbackTypeArgs = classifier.cfir.typeParameters.map {
            ConeTypeParameterType(ConeTypeParameterLookupTag(it.name.asString()))
        }
        val finalTypeArgs = when {
            resolvedTypeArgs.size == classifier.cfir.typeParameters.size -> resolvedTypeArgs
            else -> fallbackTypeArgs
        }
        return makeClassType(classifier.cfir.classKind, ConeClassLookupTagImpl(classId), finalTypeArgs)
    }

    private fun tryEnumConstructorFallback(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
        ctx: CfirResolutionContext,
    ): Boolean {
        val callInfo = buildCallInfo(
            callSite = functionCall,
            kind = CfirCallKind.EnumConstructorCall,
            name = reference.name,
            explicitReceiver = functionCall.explicitReceiver,
            arguments = functionCall.arguments,
            typeArguments = functionCall.typeArguments,
        )
        return when (val result = callResolver.resolveCallAndSelectCandidate(callInfo, ctx)) {
            is CfirCallResolutionResult.Success,
            is CfirCallResolutionResult.ResolvedWithErrors -> {
                val candidate = when (result) {
                    is CfirCallResolutionResult.Success          -> result.candidate
                    is CfirCallResolutionResult.ResolvedWithErrors -> result.candidate
                    else -> return false
                }
                functionCall.replaceCalleeReference(
                    buildAppliedCallableReference(reference.name, candidate, components)
                )
                val returnType = components.callCompleter.completedResultType(candidate)
                    ?: ConeErrorType("unresolved return type")
                functionCall.replaceConeTypeOrNull(refineResolvedCallType(returnType, ctx.expectedType))
                true
            }
            else -> false
        }
    }

    private fun resolveEnumConstructorAccess(
        access: CfirExpression,
        reference: CfirNamedReference,
        ctx: CfirResolutionContext,
    ): CfirCandidate? {
        val typeArguments = (access as? CfirQualifiedAccess)?.typeArguments ?: emptyList()
        val callInfo = buildCallInfo(
            callSite = access,
            kind = CfirCallKind.EnumConstructorCall,
            name = reference.name,
            explicitReceiver = null,
            arguments = emptyList(),
            typeArguments = typeArguments,
        )
        return when (val result = callResolver.resolveCallAndSelectCandidate(callInfo, ctx)) {
            is CfirCallResolutionResult.Success          -> result.candidate
            is CfirCallResolutionResult.ResolvedWithErrors -> result.candidate
            else                                         -> null
        }
    }

    private fun tryBuiltinOperatorFallback(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
    ): ConeCangJieType? {
        val receiverType = functionCall.explicitReceiver?.coneTypeOrNull
        val argTypes = functionCall.arguments.mapNotNull { it.coneTypeOrNull }
        return CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(reference.name, receiverType, argTypes)
    }

    /**
     * First-class function / callable-variable invoke fallback:
     * supports `f(x)` where `f` is a variable of function type, and
     * `obj.f(x)` where `f` is a function-typed member.
     */
    private fun tryCallableVariableInvokeFallback(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
    ): Boolean {
        val functionType = if (functionCall.explicitReceiver != null) {
            extractFunctionLikeType(resolveWithReceiver(reference.name, functionCall.explicitReceiver!!))
        } else {
            val variableSymbol = towerResolver.findVariables(reference.name).firstOrNull { candidate ->
                extractFunctionLikeType(extractTypeFromCallableSymbol(candidate)) != null
            } ?: return false

            val resolvedFunctionType = extractFunctionLikeType(extractTypeFromCallableSymbol(variableSymbol))
                ?: return false

            functionCall.replaceCalleeReference(
                CfirResolvedAppliedCallableReference(
                    source = null,
                    name = reference.name,
                    resolvedSymbol = variableSymbol,
                    substitutedReturnType = resolvedFunctionType.returnType,
                    substitutedParameterTypes = resolvedFunctionType.parameterTypes,
                )
            )
            resolvedFunctionType
        } ?: return false

        functionCall.arguments.forEachIndexed { i, arg ->
            val expectedParamType = functionType.parameterTypes.getOrNull(i) ?: return@forEachIndexed
            if (arg is CfirLambdaExpression) {
                arg.transform<CfirElement, CfirResolutionMode>(
                    transformer,
                    CfirResolutionMode.WithExpectedType(buildResolvedTypeRef { coneType = expectedParamType }),
                )
            }
        }
        functionCall.replaceConeTypeOrNull(functionType.returnType)
        return true
    }

    // ── Member Scope Helpers ──────────────────────────────────────────────────

    private fun resolveWithReceiver(name: Name, receiver: CfirExpression): ConeCangJieType {
        val receiverType = receiver.coneTypeOrNull
            ?: return ConeErrorType("receiver has no type")
        val memberScope = getMemberScope(receiverType)
            ?: return ConeErrorType("no member scope for type: $receiverType")

        val candidates = mutableListOf<CfirCallableSymbol<*>>()
        memberScope.processCallablesByName(name) { candidates += it }
        memberScope.processPropertiesByName(name) { candidates += it }
        memberScope.processFunctionsByName(name) { candidates += it }

        return if (candidates.isEmpty()) ConeErrorType(ConeUnresolvedNameError(name, receiverType = receiverType))
        else extractTypeFromCallableSymbol(candidates.first())
    }

    private fun getMemberScope(type: ConeCangJieType): CfirClassUseSiteMemberScope? {
        val classId = when (type) {
            is ConeClassLikeType -> type.classId
            is ConeStructType    -> type.classId
            is ConeEnumType      -> type.classId
            else                 -> return null
        }
        val classSymbol = components.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        return CfirClassUseSiteMemberScope(classSymbol, components.symbolProvider)
    }

    // ── Type Extraction ───────────────────────────────────────────────────────

    private fun extractTypeFromCallableSymbol(symbol: CfirCallableSymbol<*>): ConeCangJieType {
        if (!symbol.isBound) return ConeErrorType("unbound symbol")
        return when (val decl = symbol.cfir) {
            is CfirFunction        -> functionLikeTypeFromFunction(decl)
            is CfirProperty        -> resolvedConeTypeOf(decl.returnTypeRef, symbol)
            is CfirFieldVariable   -> resolvedConeTypeOf(decl.returnTypeRef, symbol)
            is CfirPatternVariable -> resolvedConeTypeOf(decl.returnTypeRef, symbol)
            is CfirValueParameter  -> resolvedConeTypeOf(decl.returnTypeRef, symbol)
            is CfirEnumConstructor -> resolveEnumConstructorOwnerType(symbol as? CfirEnumConstructorSymbol)
                ?.coneType ?: resolvedConeTypeOf(decl.returnTypeRef, symbol)
            else -> ConeErrorType("unsupported callable declaration: ${decl::class.simpleName}")
        }
    }

    private fun resolvedConeTypeOf(typeRef: CfirTypeRef, symbol: CfirSymbol<*>): ConeCangJieType =
        if (typeRef is CfirResolvedTypeRef) typeRef.coneType
        else ConeErrorType("unresolved type for ${symbol::class.simpleName}")

    private fun extractTypeFromSymbol(symbol: CfirSymbol<*>): ConeCangJieType = when (symbol) {
        is CfirCallableSymbol<*> -> extractTypeFromCallableSymbol(symbol)
        is CfirClassSymbol -> {
            val classId = resolveClassIdBySymbol(symbol)
                ?: return ConeErrorType("unresolved class id for symbol: ${symbol.cfir.name}")
            val typeArgs = symbol.cfir.typeParameters.map {
                ConeTypeParameterType(ConeTypeParameterLookupTag(it.name.asString()))
            }
            makeClassType(symbol.cfir.classKind, ConeClassLookupTagImpl(classId), typeArgs)
        }
        else -> ConeErrorType("unsupported symbol type: ${symbol::class.simpleName}")
    }

    private fun extractTypeFromSymbolWithExpected(
        symbol: CfirSymbol<*>,
        data: CfirResolutionMode,
    ): ConeCangJieType {
        val expectedType = data.expectedTypeOrNull
        if (symbol is CfirEnumConstructorSymbol && expectedType != null) {
            val ownerClassId = resolveEnumConstructorOwnerClassId(symbol)
            if (ownerClassId != null) {
                val refined = when (expectedType) {
                    is ConeEnumType     -> if (expectedType.classId == ownerClassId) expectedType else null
                    is ConeClassLikeType -> if (expectedType.classId == ownerClassId)
                        ConeEnumType(expectedType.lookupTag, expectedType.typeArguments) else null
                    else -> null
                }
                if (refined != null) return refined
            }
        }
        return extractTypeFromSymbol(symbol)
    }

    private fun extractReturnTypeFromSymbol(symbol: CfirSymbol<*>): ConeCangJieType {
        if (symbol is CfirFunctionSymbol && symbol.isBound) {
            val typeRef = symbol.cfir.returnTypeRef
            return if (typeRef is CfirResolvedTypeRef) typeRef.coneType
            else ConeErrorType("unresolved return type")
        }
        return extractTypeFromSymbol(symbol)
    }

    private fun refineResolvedCallType(
        returnType: ConeCangJieType,
        expectedType: ConeCangJieType?,
    ): ConeCangJieType {
        if (expectedType == null || returnType !is ConeEnumType) return returnType
        return when (expectedType) {
            is ConeEnumType     -> if (expectedType.classId == returnType.classId) expectedType else returnType
            is ConeClassLikeType -> if (expectedType.classId == returnType.classId)
                ConeEnumType(expectedType.lookupTag, expectedType.typeArguments) else returnType
            else                -> returnType
        }
    }

    private fun resolveEnumConstructorOwnerType(symbol: CfirEnumConstructorSymbol?): CfirResolvedTypeRef? {
        symbol ?: return null
        val classId = resolveEnumConstructorOwnerClassId(symbol) ?: return null
        val typeArgs = symbol.cfir.typeParameters.map {
            ConeTypeParameterType(ConeTypeParameterLookupTag(it.name.asString()))
        }
        return buildResolvedTypeRef {
            source = symbol.cfir.returnTypeRef.source
            delegatedTypeRef = symbol.cfir.returnTypeRef
            coneType = ConeEnumType(ConeClassLookupTagImpl(classId), typeArgs)
        }
    }

    private fun functionLikeTypeFromFunction(function: CfirFunction): ConeFuncType {
        val parameterTypes = function.valueParameters.mapIndexed { i, param ->
            (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                ?: ConeErrorType("unresolved parameter type at index $i")
        }
        val returnType = (function.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            ?: ConeErrorType("unresolved return type")
        return ConeFuncType(parameterTypes, returnType)
    }

    private fun extractFunctionLikeType(type: ConeCangJieType?): ConeFuncType? = type as? ConeFuncType

    // ── Stdlib / ClassId Helpers ──────────────────────────────────────────────

    private fun stdlibStringType(): ConeCangJieType {
        val symbol = components.symbolProvider.getClassLikeSymbolByClassId(StdlibClassIds.String)
        if (symbol is CfirClassSymbol && symbol.isBound) {
            val lookupTag = ConeClassLookupTagImpl(StdlibClassIds.String)
            return makeClassType(symbol.cfir.classKind, lookupTag, emptyList())
        }
        return ConeClassLikeType(ConeClassLookupTagImpl(StdlibClassIds.String))
    }

    private fun makeClassType(
        kind: CfirClassKind,
        lookupTag: ConeClassLookupTagImpl,
        typeArgs: List<ConeCangJieType>,
    ): ConeCangJieType = when (kind) {
        CfirClassKind.CLASS, CfirClassKind.INTERFACE ->
            ConeClassLikeType(lookupTag, typeArgs, isInterface = kind == CfirClassKind.INTERFACE)
        CfirClassKind.STRUCT -> ConeStructType(lookupTag, typeArgs)
        CfirClassKind.ENUM   -> ConeEnumType(lookupTag, typeArgs)
    }

    private fun resolveClassIdBySymbol(symbol: CfirClassSymbol): org.cangnova.cangjie.name.ClassId? =
        session.symbolProvider.getClassIdBySymbol(symbol) ?: session.cfirProvider.getClassIdBySymbol(symbol)

    private fun resolveEnumConstructorOwnerClassId(
        symbol: CfirEnumConstructorSymbol,
    ): org.cangnova.cangjie.name.ClassId? =
        session.symbolProvider.getEnumConstructorOwnerClassId(symbol)
            ?: session.cfirProvider.getEnumConstructorOwnerClassId(symbol)

    // ── Lambda Re-transform ───────────────────────────────────────────────────

    /**
     * After a candidate is selected, re-transform any lambda arguments using the
     * substituted parameter types so that lambda bodies are typed in the right context.
     */
    private fun retransformLambdaArguments(functionCall: CfirFunctionCall, candidate: CfirCandidate) {
        if (!candidate.symbol.isBound || !candidate.argumentMappingInitialized) return
        for ((argumentAtom, parameter) in candidate.argumentMapping) {
            val arg = argumentAtom.expression as? CfirLambdaExpression ?: continue
            val rawParamType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            val paramType = candidate.substitutor.substituteOrSelf(rawParamType) as? ConeFuncType ?: continue
            arg.transform<CfirElement, CfirResolutionMode>(
                transformer,
                CfirResolutionMode.WithExpectedType(buildResolvedTypeRef { coneType = paramType }),
            )
        }
    }

    // ── Common Supertype ──────────────────────────────────────────────────────

    private fun commonSupertype(types: List<ConeCangJieType>): ConeCangJieType {
        if (types.isEmpty()) return builtinTypes.unitType
        val first = types.first()
        if (types.all { it == first }) return first

        val nonNothing = types.filter { it != ConePrimitiveType.NOTHING }
        if (nonNothing.isEmpty()) return ConePrimitiveType.NOTHING
        if (nonNothing.size == 1) return nonNothing.first()

        val typeCheckerContext = CfirTypeCheckerContext(session)
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
        context: CfirTypeCheckerContext,
    ): List<ConeCangJieType> {
        val result = mutableListOf<ConeCangJieType>()
        val visited = mutableSetOf<ConeCangJieType>()
        val queue = ArrayDeque<ConeCangJieType>()
        queue.add(type)
        visited.add(type)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result += current
            context.supertypes(current).forEach { supertype ->
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
        return try { block() } finally { context.withTowerDataContext(saved) {} }
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
            context.withTowerDataContext(saved) {}
        }
    }

    // ── Small Extension Utilities ─────────────────────────────────────────────

    private fun CfirExpression.resolveIndependently() {
        transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
    }

    private fun CfirExpression.resolveIndependently(body: CfirBlock?) {
        body?.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
    }

    private val CfirResolutionMode.expectedTypeOrNull: ConeCangJieType?
        get() = (this as? CfirResolutionMode.WithExpectedType)?.expectedTypeRef?.coneType

    /**
     * Shared factory for [CfirCallInfo] to reduce repetition at call sites.
     */
    private fun buildCallInfo(
        callSite: CfirExpression,
        kind: CfirCallKind,
        name: Name,
        explicitReceiver: CfirExpression?,
        arguments: List<CfirExpression>,
        typeArguments: List<CfirTypeRef>,
    ) = CfirCallInfo(
        callSite = callSite,
        callKind = kind,
        name = name,
        explicitReceiver = explicitReceiver,
        arguments = arguments,
        typeArguments = typeArguments,
        session = session,
    )

    /**
     * Bind a resolved reference to any expression that exposes a mutable
     * [calleeReference] field through its implementation class.
     */
    private fun CfirExpression.bindResolvedReference(name: Name, symbol: CfirSymbol<*>) {
        val resolved = CfirResolvedNamedReferenceImpl(null, name, symbol)
        when (this) {
            is org.cangnova.cangjie.cfir.expressions.impl.CfirPropertyAccessImpl  -> calleeReference = resolved
            is org.cangnova.cangjie.cfir.expressions.impl.CfirQualifiedAccessImpl -> calleeReference = resolved
            is org.cangnova.cangjie.cfir.expressions.impl.CfirFunctionCallImpl    -> calleeReference = resolved
            else -> Unit
        }
    }

    /**
     * Resolve receiver-qualified access and mutate [this] type in-place.
     */
    private fun <T : CfirExpression> T.resolveWithReceiverInPlace(
        name: Name,
        receiver: CfirExpression,
        @Suppress("UNUSED_PARAMETER") data: CfirResolutionMode,
    ): T {
        replaceConeTypeOrNull(resolveWithReceiver(name, receiver))
        return this
    }

    private fun CfirPropertyAccess.toCallableQualifiedAccess(): CfirQualifiedAccess =
        buildQualifiedAccess {
            source = this@toCallableQualifiedAccess.source
            annotations.addAll(this@toCallableQualifiedAccess.annotations)
            coneTypeOrNull = this@toCallableQualifiedAccess.coneTypeOrNull
            calleeReference = this@toCallableQualifiedAccess.calleeReference
            explicitReceiver = this@toCallableQualifiedAccess.explicitReceiver
        }
}
