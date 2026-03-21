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
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.diagnosticReporter
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * Expression resolve transformer.
 *
 * Computes and propagates expression types, including:
 * literals, accesses, calls, patterns, control-flow expressions, and lambdas.
 */
@OptIn(CfirImplementationDetail::class)
class CfirExpressionsResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
) : CfirPartialBodyResolveTransformer(transformer) {

    private val builtinTypes get() = session.builtinTypes
    private val callResolver get() = components.callResolver
    private val towerResolver get() = components.towerResolver

    // ---- Literals ----

    override fun transformLiteralExpression(
        literalExpression: CfirLiteralExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        val synthesized = synthesizeLiteralType(literalExpression.kind)
        // Refine ideal literal types with expected type when available.
        val expectedType = (data as? CfirResolutionMode.WithExpectedType)?.expectedType
        val type = IdealTypeResolver.resolveIfIdeal(synthesized, expectedType)
        literalExpression.replaceConeTypeOrNull(type)
        return literalExpression
    }

    private fun synthesizeLiteralType(kind: CfirLiteralKind): ConeCangJieType {
        return when (kind) {
            CfirLiteralKind.INT -> ConePrimitiveType.IDEAL_INT
            CfirLiteralKind.FLOAT -> ConePrimitiveType.IDEAL_FLOAT
            CfirLiteralKind.BOOLEAN -> builtinTypes.boolType
            CfirLiteralKind.RUNE -> ConePrimitiveType.RUNE
            CfirLiteralKind.STRING -> stdlibStringType()
            CfirLiteralKind.UNIT -> builtinTypes.unitType
            CfirLiteralKind.NULL -> ConePrimitiveType.NOTHING
        }
    }

    // ---- Property / Variable Access ----

    override fun transformPropertyAccess(
        propertyAccess: CfirPropertyAccess,
        data: CfirResolutionMode,
    ): CfirExpression {
        // Resolve explicit receiver first.
        val receiver = propertyAccess.explicitReceiver
        receiver?.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)

        val reference = propertyAccess.calleeReference
        if (reference is CfirResolvedNamedReference) {
            val expectedType = (data as? CfirResolutionMode.WithExpectedType)?.expectedType
            if (expectedType == null) {
                val currentType = propertyAccess.coneTypeOrNull
                if (currentType != null && currentType !is ConeErrorType) {
                    return propertyAccess
                }
            }
            // Already resolved reference, use symbol type directly.
            val resolvedSymbol = reference.resolvedSymbol
            propertyAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(resolvedSymbol, data))
            return propertyAccess
        }

        if (reference !is CfirNamedReference) {
            propertyAccess.replaceConeTypeOrNull(ConeErrorType("non-name reference"))
            return propertyAccess
        }

        val name = reference.name

        if (receiver != null) {
            // With receiver, resolve in member scope.
            val resolvedType = resolveWithReceiver(name, receiver)
            propertyAccess.replaceConeTypeOrNull(resolvedType)
        } else {
            val expectedType = (data as? CfirResolutionMode.WithExpectedType)?.expectedType
            val callables = towerResolver.findCallables(name)
            if (callables.isNotEmpty()) {
                val resolutionContext = components.createResolutionContext(expectedType)
                if (resolutionContext != null) {
                    val callableAccess = propertyAccess.toCallableQualifiedAccess()
                    applyCallableAccessResolutionResult(
                        access = callableAccess,
                        reference = reference,
                        resolutionResult = resolveCallableAccessWithoutReceiverWithPhase3(
                            access = callableAccess,
                            reference = reference,
                            typeArguments = emptyList(),
                            resolutionContext = resolutionContext,
                        ),
                        expectedType = expectedType,
                        resolutionContext = resolutionContext,
                    )
                    return callableAccess
                }
            }

            // Without receiver, resolve from tower scopes.
            val candidates = towerResolver.findVariables(name)
            if (candidates.isNotEmpty()) {
                val symbol = candidates.first()
                (propertyAccess as? org.cangnova.cangjie.cfir.expressions.impl.CfirPropertyAccessImpl)?.calleeReference =
                    CfirResolvedNamedReferenceImpl(null, name, symbol)
                propertyAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(symbol, data))
                return propertyAccess
            }

                val classifier = towerResolver.findClassifiers(name).firstOrNull()
            if (classifier != null) {
                (propertyAccess as? org.cangnova.cangjie.cfir.expressions.impl.CfirPropertyAccessImpl)?.calleeReference =
                    CfirResolvedNamedReferenceImpl(null, name, classifier)
                propertyAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(classifier, data))
                return propertyAccess
            }

            if (callables.isNotEmpty()) {
                val symbol = callables.first()
                (propertyAccess as? org.cangnova.cangjie.cfir.expressions.impl.CfirPropertyAccessImpl)?.calleeReference =
                    CfirResolvedNamedReferenceImpl(null, name, symbol)
                propertyAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(symbol, data))
            } else {
                propertyAccess.replaceConeTypeOrNull(ConeErrorType(ConeUnresolvedReferenceError(name)))
            }
        }
        return propertyAccess
    }

    // ---- Qualified Access ----

    override fun transformQualifiedAccess(
        qualifiedAccess: CfirQualifiedAccess,
        data: CfirResolutionMode,
    ): CfirExpression {
        // Resolve explicit receiver first.
        val receiver = qualifiedAccess.explicitReceiver
        receiver?.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)

        val reference = qualifiedAccess.calleeReference
        if (reference is CfirResolvedNamedReference) {
            val expectedType = (data as? CfirResolutionMode.WithExpectedType)?.expectedType
            if (expectedType == null) {
                val currentType = qualifiedAccess.coneTypeOrNull
                if (currentType != null && currentType !is ConeErrorType) {
                    return qualifiedAccess
                }
            }
            val resolvedSymbol = reference.resolvedSymbol
            qualifiedAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(resolvedSymbol, data))
            return qualifiedAccess
        }

        if (reference !is CfirNamedReference) {
            qualifiedAccess.replaceConeTypeOrNull(ConeErrorType("non-name reference"))
            return qualifiedAccess
        }

        val name = reference.name

        if (receiver != null) {
            val resolvedType = resolveWithReceiver(name, receiver)
            qualifiedAccess.replaceConeTypeOrNull(resolvedType)
        } else {
            val candidates = towerResolver.findVariables(name)
            if (candidates.isNotEmpty()) {
                val symbol = candidates.first()
                (qualifiedAccess as? org.cangnova.cangjie.cfir.expressions.impl.CfirQualifiedAccessImpl)?.calleeReference =
                    CfirResolvedNamedReferenceImpl(null, name, symbol)
                qualifiedAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(symbol, data))
                return qualifiedAccess
            }

            val classifier = towerResolver.findClassifiers(name).firstOrNull()
            if (classifier != null) {
                (qualifiedAccess as? org.cangnova.cangjie.cfir.expressions.impl.CfirQualifiedAccessImpl)?.calleeReference =
                    CfirResolvedNamedReferenceImpl(null, name, classifier)
                qualifiedAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(classifier, data))
                return qualifiedAccess
            }

            val callables = towerResolver.findCallables(name)
            if (callables.isNotEmpty()) {
                val expectedType = (data as? CfirResolutionMode.WithExpectedType)?.expectedType
                val resolutionContext = components.createResolutionContext(expectedType)
                if (resolutionContext != null) {
                    applyCallableAccessResolutionResult(
                        access = qualifiedAccess,
                        reference = reference,
                        resolutionResult = resolveCallableAccessWithoutReceiverWithPhase3(
                            access = qualifiedAccess,
                            reference = reference,
                            typeArguments = qualifiedAccess.typeArguments,
                            resolutionContext = resolutionContext,
                        ),
                        expectedType = expectedType,
                        resolutionContext = resolutionContext,
                    )
                    return qualifiedAccess
                }

                val symbol = callables.first()
                (qualifiedAccess as? org.cangnova.cangjie.cfir.expressions.impl.CfirQualifiedAccessImpl)?.calleeReference =
                    CfirResolvedNamedReferenceImpl(null, name, symbol)
                qualifiedAccess.replaceConeTypeOrNull(extractTypeFromSymbolWithExpected(symbol, data))
            } else {
                qualifiedAccess.replaceConeTypeOrNull(ConeErrorType(ConeUnresolvedReferenceError(name)))
            }
        }
        return qualifiedAccess
    }

    // ---- Function Call ----

    override fun transformFunctionCall(
        functionCall: CfirFunctionCall,
        data: CfirResolutionMode,
    ): CfirExpression {
        // Resolve callee and arguments first.
        functionCall.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        val reference = functionCall.calleeReference
        if (reference is CfirResolvedNamedReference) {
            val appliedReturnType = (reference as? CfirResolvedAppliedCallableReference)?.substitutedReturnType
            functionCall.replaceConeTypeOrNull(appliedReturnType ?: extractReturnTypeFromSymbol(reference.resolvedSymbol))
            return functionCall
        }

        if (reference !is CfirNamedReference) {
            functionCall.replaceConeTypeOrNull(ConeErrorType("non-name callee reference"))
            return functionCall
        }

        // Phase 3: resolve with call context that includes expected type.
        val expectedType = (data as? CfirResolutionMode.WithExpectedType)?.expectedType
        val resolutionContext = components.createResolutionContext(expectedType)
        if (resolutionContext != null) {
            return resolveCallWithPhase3(functionCall, reference, resolutionContext)
        }

        // Fallback to legacy path.
        return resolveCallLegacy(functionCall, reference)
    }

    /**
     * Phase-3 call resolution path using call info + staged resolver.
     */
    private fun resolveCallWithPhase3(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
        resolutionContext: org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext,
    ): CfirExpression {
        val expectedType = resolutionContext.expectedType
        val callInfo = CfirCallInfo(
            callSite = functionCall,
            callKind = CfirCallKind.Function,
            name = reference.name,
            explicitReceiver = functionCall.explicitReceiver,
            arguments = functionCall.arguments,
            typeArguments = functionCall.typeArguments,
            session = session,
        )

        when (val result = callResolver.resolveCallAndSelectCandidate(callInfo, resolutionContext)) {
            is CfirCallResolutionResult.Success -> {
                val candidate = result.candidate
                retransformLambdaArguments(functionCall, candidate)
                functionCall.replaceCalleeReference(buildAppliedCallableReference(reference.name, candidate))
                val returnType = candidate.resolvedReturnType() ?: ConeErrorType("unresolved return type")
                functionCall.replaceConeTypeOrNull(refineResolvedCallType(returnType, expectedType))
            }
            is CfirCallResolutionResult.ResolvedWithErrors -> {
                val candidate = result.candidate
                retransformLambdaArguments(functionCall, candidate)
                functionCall.replaceCalleeReference(buildAppliedCallableReference(reference.name, candidate))
                val returnType = candidate.resolvedReturnType() ?: ConeErrorType("resolved with errors")
                functionCall.replaceConeTypeOrNull(refineResolvedCallType(returnType, expectedType))
            }
            is CfirCallResolutionResult.Ambiguity -> {
                functionCall.replaceConeTypeOrNull(ConeErrorType("ambiguous call: ${reference.name}"))
            }
            is CfirCallResolutionResult.NoCandidate -> {
                if (tryClassifierQualifierFallback(functionCall, reference)) {
                    return functionCall
                }
                if (tryEnumConstructorFallback(functionCall, reference, resolutionContext)) {
                    return functionCall
                }
                if (tryCallableVariableInvokeFallback(functionCall, reference)) {
                    return functionCall
                }
                // Phase 5: builtin operator fallback.
                val builtinResult = tryBuiltinOperatorFallback(functionCall, reference)
                if (builtinResult != null) {
                    functionCall.replaceConeTypeOrNull(builtinResult)
                } else {
                    functionCall.replaceConeTypeOrNull(ConeErrorType(ConeUnresolvedNameError(reference.name)))
                }
            }
            is CfirCallResolutionResult.LegacySuccess -> {
                (functionCall as? org.cangnova.cangjie.cfir.expressions.impl.CfirFunctionCallImpl)?.calleeReference =
                    CfirResolvedNamedReferenceImpl(null, reference.name, result.symbol)
                functionCall.replaceConeTypeOrNull(result.returnType)
            }
            is CfirCallResolutionResult.LegacyAmbiguity -> {
                functionCall.replaceConeTypeOrNull(ConeErrorType("ambiguous call: ${reference.name}"))
            }
        }
        return functionCall
    }

    private fun resolveCallableAccessWithoutReceiverWithPhase3(
        access: CfirExpression,
        reference: CfirNamedReference,
        typeArguments: List<CfirTypeRef>,
        resolutionContext: CfirResolutionContext,
    ): CfirCallResolutionResult {
        val callInfo = CfirCallInfo(
            callSite = access,
            callKind = CfirCallKind.VariableAccess,
            name = reference.name,
            explicitReceiver = null,
            arguments = emptyList(),
            typeArguments = typeArguments,
            session = session,
        )
        return callResolver.resolveCallAndSelectCandidate(callInfo, resolutionContext)
    }

    private fun CfirPropertyAccess.toCallableQualifiedAccess(): CfirQualifiedAccess {
        return buildQualifiedAccess {
            source = this@toCallableQualifiedAccess.source
            annotations.addAll(this@toCallableQualifiedAccess.annotations)
            coneTypeOrNull = this@toCallableQualifiedAccess.coneTypeOrNull
            calleeReference = this@toCallableQualifiedAccess.calleeReference
            explicitReceiver = this@toCallableQualifiedAccess.explicitReceiver
        }
    }

    private fun <T> applyCallableAccessResolutionResult(
        access: T,
        reference: CfirNamedReference,
        resolutionResult: CfirCallResolutionResult,
        expectedType: ConeCangJieType?,
        resolutionContext: CfirResolutionContext? = null,
    ) where T : CfirExpression, T : org.cangnova.cangjie.cfir.CfirResolvable {
        when (resolutionResult) {
            is CfirCallResolutionResult.Success -> {
                applyResolvedCallableAccessCandidate(access, reference, resolutionResult.candidate, expectedType)
            }
            is CfirCallResolutionResult.ResolvedWithErrors -> {
                applyResolvedCallableAccessCandidate(access, reference, resolutionResult.candidate, expectedType)
            }
            is CfirCallResolutionResult.Ambiguity -> {
                access.replaceConeTypeOrNull(ConeErrorType("ambiguous call: ${reference.name}"))
            }
            is CfirCallResolutionResult.NoCandidate -> {
                // 枚举构造器 fallback（变量式，如 None / None<Int64>）
                if (resolutionContext != null) {
                    val enumResult = resolveEnumConstructorAccess(access, reference, resolutionContext)
                    if (enumResult != null) {
                        applyResolvedCallableAccessCandidate(access, reference, enumResult, expectedType)
                        return
                    }
                }
                access.replaceConeTypeOrNull(ConeErrorType(ConeUnresolvedReferenceError(reference.name)))
            }
            is CfirCallResolutionResult.LegacySuccess -> {
                access.replaceConeTypeOrNull(resolutionResult.returnType)
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
        access.replaceCalleeReference(buildAppliedCallableReference(reference.name, candidate))
        val returnType = candidate.resolvedReturnType() ?: ConeErrorType("unresolved callable access type")
        access.replaceConeTypeOrNull(refineResolvedCallType(returnType, expectedType))
    }

    /**
     * Legacy call resolution path used when no phase-3 context is available.
     */
    private fun resolveCallLegacy(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
    ): CfirExpression {
        when (val result = callResolver.resolveCall(reference.name, functionCall.arguments)) {
            is CfirCallResolutionResult.LegacySuccess -> {
                (functionCall as? org.cangnova.cangjie.cfir.expressions.impl.CfirFunctionCallImpl)?.calleeReference =
                    CfirResolvedNamedReferenceImpl(null, reference.name, result.symbol)
                functionCall.replaceConeTypeOrNull(result.returnType)
            }
            is CfirCallResolutionResult.LegacyAmbiguity -> {
                functionCall.replaceConeTypeOrNull(ConeErrorType("ambiguous call: ${reference.name}"))
            }
            is CfirCallResolutionResult.NoCandidate -> {
                if (tryClassifierQualifierFallback(functionCall, reference)) {
                    return functionCall
                }
                if (tryCallableVariableInvokeFallback(functionCall, reference)) {
                    return functionCall
                }
                functionCall.replaceConeTypeOrNull(ConeErrorType(ConeUnresolvedNameError(reference.name)))
            }
            else -> {
                functionCall.replaceConeTypeOrNull(ConeErrorType("unexpected resolution result"))
            }
        }
        return functionCall
    }

    // ---- Block ----

    override fun transformBlock(block: CfirBlock, data: CfirResolutionMode): CfirExpression {
        block.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // Block type is the last expression type, or Unit when absent/non-expression.
        val lastExpr = block.statements.lastOrNull()
        val blockType = if (lastExpr is CfirExpression) {
            lastExpr.coneTypeOrNull ?: builtinTypes.unitType
        } else {
            builtinTypes.unitType
        }
        block.replaceConeTypeOrNull(blockType)
        return block
    }

    // ---- Match ----

    /**
     * Resolve match in branch-local scopes:
     * 1) resolve subject;
     * 2) resolve each branch pattern/guard/body;
     * 3) compute result type from branch body types.
     */
    override fun transformMatchExpression(
        matchExpression: CfirMatchExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        // Match needs branch-local bindings, so resolve the subject first and
        // then process each branch in its own local scope.
        matchExpression.subject?.transform<CfirElement, CfirResolutionMode>(
            transformer,
            CfirResolutionMode.ContextIndependent,
        )

        val subjectType = matchExpression.subject?.coneTypeOrNull

        val branchTypes = mutableListOf<ConeCangJieType>()
        matchExpression.branches.forEach { branch ->
            resolveBranch(branch, subjectType, branchTypes)
        }

        val resultType = computeMatchResultType(branchTypes)
        matchExpression.replaceConeTypeOrNull(resultType)
        return matchExpression
    }

    /** Resolve a single match branch in an isolated local scope. */
    private fun resolveBranch(
        branch: CfirMatchBranch,
        subjectType: ConeCangJieType?,
        branchTypes: MutableList<ConeCangJieType>,
    ): CfirMatchBranch {
        val savedContext = context.towerDataContext
        val branchScope = CfirLocalScopeImpl()
        context.addLocalScope(branchScope)

        try {
            branch.transformPattern(transformer, CfirResolutionMode.ContextIndependent)
            resolvePattern(branch.pattern, subjectType)

            branch.transformGuard(transformer, CfirResolutionMode.ContextIndependent)
            val guard = branch.guard
            if (guard != null) {
                val guardType = guard.coneTypeOrNull
                val expectedGuardType = builtinTypes.boolType
                if (guardType != null && !components.subtypeChecker.isSubtypeOf(guardType, expectedGuardType)) {
                    val source = guard.source as? AbstractCjSourceElement
                    if (source != null) {
                        session.diagnosticReporter.reportOn(
                            source,
                            CfirErrors.TYPE_MISMATCH,
                            expectedGuardType,
                            guardType,
                            false,
                            matchDiagnosticContext(),
                        )
                    }
                }
            }

            branch.transformBody(transformer, CfirResolutionMode.ContextIndependent)

            val bodyType = branch.body.coneTypeOrNull ?: builtinTypes.unitType
            branch.replaceConeTypeOrNull(bodyType)
            branchTypes.add(bodyType)
        } finally {
            context.withTowerDataContext(savedContext) {}
        }

        return branch
    }

    private fun matchDiagnosticContext(): DiagnosticContext {
        val filePath = context.file.sourceFile?.path
        return object : DiagnosticContext {
            override val languageVersionSettings = org.cangnova.cangjie.LanguageVersionSettings.DEFAULT
            override val containingFilePath: String? = filePath
            override fun isDiagnosticSuppressed(diagnostic: org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic): Boolean = false
        }
    }

    /**
     * Resolve pattern bindings and nested patterns with expected type propagation.
     */
    private fun resolvePattern(pattern: CfirPattern, expectedType: ConeCangJieType?) {
        when (pattern) {
            is CfirWildcardPattern -> Unit
            is CfirConstPattern -> Unit
            is CfirBindingPattern -> {
                val bindingType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType ?: expectedType
                if (bindingType != null) {
                    storePatternBinding(pattern.name, bindingType)
                }
                pattern.nestedPattern?.let { nested -> resolvePattern(nested, expectedType) }
            }
            is CfirTuplePattern -> {
                val tupleType = expectedType as? ConeTupleType
                pattern.elements.forEachIndexed { index, subPattern ->
                    resolvePattern(subPattern, tupleType?.elementTypes?.getOrNull(index))
                }
            }
            is CfirEnumPattern -> {
                pattern.arguments.forEach { argPattern -> resolvePattern(argPattern, null) }
            }
            is CfirTypePattern -> {
                val patternType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType
                if (patternType != null && pattern.bindingName != null) {
                    storePatternBinding(pattern.bindingName!!, patternType)
                }
            }
            is CfirExpressionPattern -> {
                pattern.expression.coneTypeOrNull
            }
            is CfirOrPattern -> {
                pattern.alternatives.forEach { alternative ->
                    val savedContext = context.towerDataContext
                    val temporaryScope = CfirLocalScopeImpl()
                    context.addLocalScope(temporaryScope)
                    try {
                        resolvePattern(alternative, expectedType)
                    } finally {
                        context.withTowerDataContext(savedContext) {}
                    }
                }
            }
        }
    }

    /** Store pattern binding as a local variable in current scope. */
    private fun storePatternBinding(name: Name, type: ConeCangJieType) {
        val symbol = CfirFieldVariableSymbol(CallableId(name))
        buildFieldVariable {
            this.name = name
            this.symbol = symbol
            this.moduleData = context.file.moduleData
            this.origin = CfirDeclarationOrigin.Source
            this.attributes = CfirDeclarationAttributes.EMPTY
            this.status = CfirDeclarationStatusImpl()
            this.returnTypeRef = buildResolvedTypeRef {
                coneType = type
            }
            this.isVar = false
        }
        context.storeVariable(name, symbol)
    }

    /** Compute match result type from branch types; fall back to union when needed. */
    private fun computeMatchResultType(branchTypes: List<ConeCangJieType>): ConeCangJieType {
        if (branchTypes.isEmpty()) return builtinTypes.unitType
        if (branchTypes.size == 1) return branchTypes.single()

        val first = branchTypes.first()
        if (branchTypes.all { it == first }) return first

        // No common exact type, use union.
        return ConeUnionType(branchTypes.toSet())
    }

    // ---- If ----

    override fun transformIfExpression(
        ifExpression: CfirIfExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        ifExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        val thenType = ifExpression.thenBranch.coneTypeOrNull
        val elseType = ifExpression.elseBranch?.let { it.coneTypeOrNull }

        val resultType = when {
            thenType == null -> elseType ?: builtinTypes.unitType
            elseType == null -> builtinTypes.unitType
            thenType == elseType -> thenType
            else -> ConeUnionType(setOf(thenType, elseType))
        }
        ifExpression.replaceConeTypeOrNull(resultType)
        return ifExpression
    }

    // ---- Return ----

    override fun transformReturnExpression(
        returnExpression: CfirReturnExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        returnExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // `return` expression type is Nothing.
        returnExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        return returnExpression
    }

    // ---- Assignment ----

    override fun transformAssignment(
        assignment: CfirAssignment,
        data: CfirResolutionMode,
    ): CfirExpression {
        // Resolve both lValue and rValue.
        assignment.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // Assignment expression type is Unit.
        assignment.replaceConeTypeOrNull(builtinTypes.unitType)
        return assignment
    }

    // ---- Tuple Literal ----

    override fun transformTupleLiteral(
        tupleLiteral: CfirTupleLiteral,
        data: CfirResolutionMode,
    ): CfirExpression {
        tupleLiteral.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        val elementTypes = tupleLiteral.elements.map { it.coneTypeOrNull ?: ConeErrorType("unresolved element") }
        tupleLiteral.replaceConeTypeOrNull(ConeTupleType(elementTypes))
        return tupleLiteral
    }

    // ---- Array Literal ----

    override fun transformArrayLiteral(
        arrayLiteral: CfirArrayLiteral,
        data: CfirResolutionMode,
    ): CfirExpression {
        arrayLiteral.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        val elementTypes = arrayLiteral.elements.mapNotNull { it.coneTypeOrNull }
        // Keep simple array inference: take first element type.
        val elementType = elementTypes.firstOrNull() ?: ConeErrorType("empty array literal")
        arrayLiteral.replaceConeTypeOrNull(ConeArrayType(elementType))
        return arrayLiteral
    }

    // ---- String Interpolation ----

    override fun transformStringInterpolation(
        stringInterpolation: CfirStringInterpolation,
        data: CfirResolutionMode,
    ): CfirExpression {
        stringInterpolation.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        stringInterpolation.replaceConeTypeOrNull(stdlibStringType())
        return stringInterpolation
    }

    private fun stdlibStringType(): ConeCangJieType {
        val symbol = components.symbolProvider.getClassLikeSymbolByClassId(StdlibClassIds.String)
        if (symbol is CfirClassSymbol && symbol.isBound) {
            val lookupTag = ConeClassLookupTagImpl(StdlibClassIds.String)
            return when (symbol.cfir.classKind) {
                CfirClassKind.CLASS, CfirClassKind.INTERFACE -> ConeClassLikeType(
                    lookupTag = lookupTag,
                    typeArguments = emptyList(),
                    isInterface = symbol.cfir.classKind == CfirClassKind.INTERFACE,
                )
                CfirClassKind.STRUCT -> ConeStructType(lookupTag, emptyList())
                CfirClassKind.ENUM -> ConeEnumType(lookupTag, emptyList())
            }
        }
        return ConeClassLikeType(ConeClassLookupTagImpl(StdlibClassIds.String))
    }

    // ---- Comparison ----

    override fun transformComparisonExpression(
        comparisonExpression: CfirComparisonExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        comparisonExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        val leftType = comparisonExpression.left.coneTypeOrNull
        val rightType = comparisonExpression.right.coneTypeOrNull

        if (leftType != null && rightType != null) {
            val opFuncName = comparisonExpression.operation.toFunctionName()
            val result = CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
                Name.identifier(opFuncName), leftType, listOf(rightType)
            )
            if (result != null) {
                comparisonExpression.replaceConeTypeOrNull(result)
                return comparisonExpression
            }
        }

        // Fallback type for comparison is Bool.
         comparisonExpression.replaceConeTypeOrNull(builtinTypes.boolType)
        return comparisonExpression
    }

    // ---- Binary Operations ----

    override fun transformBinaryOp(
        binaryOp: CfirBinaryOp,
        data: CfirResolutionMode,
    ): CfirExpression {
        binaryOp.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        val resultType = when (binaryOp.kind) {
            CfirBinaryOpKind.AND, CfirBinaryOpKind.OR -> builtinTypes.boolType
            CfirBinaryOpKind.COALESCING -> binaryOp.left.coneTypeOrNull ?: ConeErrorType("unresolved coalescing left")
            CfirBinaryOpKind.PIPELINE -> binaryOp.right.coneTypeOrNull ?: ConeErrorType("unresolved pipeline right")
        }
        binaryOp.replaceConeTypeOrNull(resultType)
        return binaryOp
    }

    // ---- Type Operator ----

    override fun transformTypeOperator(
        typeOperator: CfirTypeOperator,
        data: CfirResolutionMode,
    ): CfirExpression {
        // Resolve argument before operator typing.
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

    // ---- Error Expression ----

    override fun transformErrorExpression(
        errorExpression: CfirErrorExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        errorExpression.replaceConeTypeOrNull(ConeErrorType(errorExpression.reason))
        return errorExpression
    }

    // ---- Helpers ----

    /**
     * Resolve member property/function type through receiver member scope.
     */
    private fun resolveWithReceiver(name: Name, receiver: CfirExpression): ConeCangJieType {
        val receiverType = receiver.coneTypeOrNull
            ?: return ConeErrorType("receiver has no type")

        val memberScope = getMemberScope(receiverType)
            ?: return ConeErrorType("no member scope for type: $receiverType")

        val candidates = mutableListOf<CfirCallableSymbol<*>>()
        memberScope.processCallablesByName(name) { candidates.add(it) }
        memberScope.processPropertiesByName(name) { candidates.add(it) }
        memberScope.processFunctionsByName(name) { candidates.add(it) }

        return if (candidates.isEmpty()) {
            ConeErrorType(ConeUnresolvedNameError(name, receiverType = receiverType))
        } else {
            extractTypeFromCallableSymbol(candidates.first())
        }
    }

    /** Build member scope from type when supported. */
    private fun getMemberScope(type: ConeCangJieType): CfirClassUseSiteMemberScope? {
        val classId = when (type) {
            is ConeClassLikeType -> type.classId
            is ConeStructType -> type.classId
            is ConeEnumType -> type.classId
            else -> return null
        }
        val classSymbol = components.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        return CfirClassUseSiteMemberScope(classSymbol, components.symbolProvider)
    }

    /** Extract callable declared type from bound symbol. */
    private fun extractTypeFromCallableSymbol(symbol: CfirCallableSymbol<*>): ConeCangJieType {
        if (!symbol.isBound) return ConeErrorType("unbound symbol")
        val typeRef = when (val decl = symbol.cfir) {
            is CfirFunction -> return functionLikeTypeFromFunction(decl)
            is CfirProperty -> decl.returnTypeRef
            is CfirFieldVariable -> decl.returnTypeRef
            is CfirPatternVariable -> decl.returnTypeRef
            is CfirValueParameter -> decl.returnTypeRef
            is CfirEnumConstructor -> {
                resolveEnumConstructorOwnerType(symbol as? CfirEnumConstructorSymbol)
                    ?: decl.returnTypeRef
            }
            else -> return ConeErrorType("unsupported callable declaration: ${decl::class.simpleName}")
        }
        return if (typeRef is CfirResolvedTypeRef) {
            typeRef.coneType
        } else {
            ConeErrorType("unresolved type for ${symbol::class.simpleName}")
        }
    }

    /** Extract symbol type for diagnostics/fallback typing. */
    private fun extractTypeFromSymbol(symbol: CfirSymbol<*>): ConeCangJieType {
        return when (symbol) {
            is CfirCallableSymbol<*> -> extractTypeFromCallableSymbol(symbol)
            is CfirClassSymbol -> {
                val classId = resolveClassIdBySymbol(symbol)
                    ?: return ConeErrorType("unresolved class id for symbol: ${symbol.cfir.name}")
                val typeArgs = symbol.cfir.typeParameters.map {
                    ConeTypeParameterType(ConeTypeParameterLookupTag(it.name.asString()))
                }
                val lookupTag = ConeClassLookupTagImpl(classId)
                when (symbol.cfir.classKind) {
                    CfirClassKind.CLASS, CfirClassKind.INTERFACE -> ConeClassLikeType(
                        lookupTag = lookupTag,
                        typeArguments = typeArgs,
                        isInterface = symbol.cfir.classKind == CfirClassKind.INTERFACE,
                    )
                    CfirClassKind.STRUCT -> ConeStructType(lookupTag, typeArgs)
                    CfirClassKind.ENUM -> ConeEnumType(lookupTag, typeArgs)
                }
            }
            else -> ConeErrorType("unsupported symbol type: ${symbol::class.simpleName}")
        }
    }

    private fun extractTypeFromSymbolWithExpected(
        symbol: CfirSymbol<*>,
        data: CfirResolutionMode,
    ): ConeCangJieType {
        val expectedType = (data as? CfirResolutionMode.WithExpectedType)?.expectedType
        if (symbol is CfirEnumConstructorSymbol && expectedType != null) {
            val ownerClassId = resolveEnumConstructorOwnerClassId(symbol)
            if (ownerClassId != null) {
                when (expectedType) {
                    is ConeEnumType -> if (expectedType.classId == ownerClassId) return expectedType
                    is ConeClassLikeType -> if (expectedType.classId == ownerClassId) {
                        return ConeEnumType(expectedType.lookupTag, expectedType.typeArguments)
                    }
                    else -> Unit
                }
            }
        }
        return extractTypeFromSymbol(symbol)
    }

    private fun tryClassifierQualifierFallback(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
    ): Boolean {
        if (functionCall.explicitReceiver != null) return false
        if (functionCall.arguments.isNotEmpty()) return false

        val classifier = towerResolver.findClassifiers(reference.name).firstOrNull() ?: return false
        val functionCallImpl =
            functionCall as? org.cangnova.cangjie.cfir.expressions.impl.CfirFunctionCallImpl ?: return false

        val qualifierType = buildClassifierQualifierType(classifier, functionCall.typeArguments)

        functionCallImpl.calleeReference = CfirResolvedNamedReferenceImpl(
            source = null,
            name = reference.name,
            resolvedSymbol = classifier,
        )
        functionCall.replaceConeTypeOrNull(qualifierType)
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
            resolvedTypeArgs.isEmpty() -> fallbackTypeArgs
            resolvedTypeArgs.size == classifier.cfir.typeParameters.size -> resolvedTypeArgs
            else -> fallbackTypeArgs
        }
        val lookupTag = ConeClassLookupTagImpl(classId)
        return when (classifier.cfir.classKind) {
            CfirClassKind.CLASS, CfirClassKind.INTERFACE -> ConeClassLikeType(
                lookupTag = lookupTag,
                typeArguments = finalTypeArgs,
                isInterface = classifier.cfir.classKind == CfirClassKind.INTERFACE,
            )
            CfirClassKind.STRUCT -> ConeStructType(lookupTag, finalTypeArgs)
            CfirClassKind.ENUM -> ConeEnumType(lookupTag, finalTypeArgs)
        }
    }

    private fun refineResolvedCallType(
        returnType: ConeCangJieType,
        expectedType: ConeCangJieType?,
    ): ConeCangJieType {
        val expected = expectedType ?: return returnType
        if (returnType !is ConeEnumType) return returnType
        return when (expected) {
            is ConeEnumType -> if (expected.classId == returnType.classId) expected else returnType
            is ConeClassLikeType -> if (expected.classId == returnType.classId) {
                ConeEnumType(expected.lookupTag, expected.typeArguments)
            } else {
                returnType
            }
            else -> returnType
        }
    }

    private fun resolveEnumConstructorOwnerType(symbol: CfirEnumConstructorSymbol?): CfirResolvedTypeRef? {
        symbol ?: return null
        val classId = resolveEnumConstructorOwnerClassId(symbol) ?: return null
        val typeArgs = symbol.cfir.typeParameters.map {
            ConeTypeParameterType(ConeTypeParameterLookupTag(it.name.asString()))
        }
        val coneType = ConeEnumType(ConeClassLookupTagImpl(classId), typeArgs)
        return buildResolvedTypeRef {
            source = symbol.cfir.returnTypeRef.source
            delegatedTypeRef = symbol.cfir.returnTypeRef
            this.coneType = coneType
        }
    }

    /**
     * 枚举构造器 fallback：当函数/变量解析失败后，
     * 使用 [CfirCallKind.EnumConstructorCall] 重新解析枚举构造器。
     */
    private fun tryEnumConstructorFallback(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
        resolutionContext: CfirResolutionContext,
    ): Boolean {
        val callInfo = CfirCallInfo(
            callSite = functionCall,
            callKind = CfirCallKind.EnumConstructorCall,
            name = reference.name,
            explicitReceiver = functionCall.explicitReceiver,
            arguments = functionCall.arguments,
            typeArguments = functionCall.typeArguments,
            session = session,
        )
        val expectedType = resolutionContext.expectedType
        when (val result = callResolver.resolveCallAndSelectCandidate(callInfo, resolutionContext)) {
            is CfirCallResolutionResult.Success -> {
                val candidate = result.candidate
                functionCall.replaceCalleeReference(buildAppliedCallableReference(reference.name, candidate))
                val returnType = candidate.resolvedReturnType() ?: ConeErrorType("unresolved return type")
                functionCall.replaceConeTypeOrNull(refineResolvedCallType(returnType, expectedType))
                return true
            }
            is CfirCallResolutionResult.ResolvedWithErrors -> {
                val candidate = result.candidate
                functionCall.replaceCalleeReference(buildAppliedCallableReference(reference.name, candidate))
                val returnType = candidate.resolvedReturnType() ?: ConeErrorType("resolved with errors")
                functionCall.replaceConeTypeOrNull(refineResolvedCallType(returnType, expectedType))
                return true
            }
            else -> return false
        }
    }

    /**
     * 枚举构造器变量式访问 fallback（如 `None`、`None<Int64>`）。
     * 使用 [CfirCallKind.EnumConstructorCall] 重新解析。
     * @return 成功的候选，或 null 表示未找到枚举构造器。
     */
    private fun resolveEnumConstructorAccess(
        access: CfirExpression,
        reference: CfirNamedReference,
        resolutionContext: CfirResolutionContext,
    ): CfirCandidate? {
        val typeArguments = when (access) {
            is CfirQualifiedAccess -> access.typeArguments
            else -> emptyList()
        }
        val callInfo = CfirCallInfo(
            callSite = access,
            callKind = CfirCallKind.EnumConstructorCall,
            name = reference.name,
            explicitReceiver = null,
            arguments = emptyList(),
            typeArguments = typeArguments,
            session = session,
        )
        return when (val result = callResolver.resolveCallAndSelectCandidate(callInfo, resolutionContext)) {
            is CfirCallResolutionResult.Success -> result.candidate
            is CfirCallResolutionResult.ResolvedWithErrors -> result.candidate
            else -> null
        }
    }

    /** Try builtin operator resolver for unresolved call. */
    private fun tryBuiltinOperatorFallback(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
    ): ConeCangJieType? {
        val receiverType = functionCall.explicitReceiver?.coneTypeOrNull
        val argTypes = functionCall.arguments.mapNotNull { it.coneTypeOrNull }
        return CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
            reference.name, receiverType, argTypes
        )
    }

    /**
     * First-class function fallback:
     * 1) support `f(x)` where `f` is a variable/parameter of function type;
     * 2) support `obj.f(x)` where `f` member itself is function-typed.
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
                ),
            )
            resolvedFunctionType
        } ?: return false

        functionCall.arguments.forEachIndexed { index, argument ->
            val expectedParamType = functionType.parameterTypes.getOrNull(index) ?: return@forEachIndexed
            if (argument is CfirLambdaExpression) {
                argument.transform<CfirElement, CfirResolutionMode>(
                    transformer,
                    CfirResolutionMode.WithExpectedType(expectedParamType),
                )
            }
        }

        functionCall.replaceConeTypeOrNull(functionType.returnType)
        return true
    }

    private fun resolveClassIdBySymbol(symbol: CfirClassSymbol): org.cangnova.cangjie.name.ClassId? {
        return session.symbolProvider.getClassIdBySymbol(symbol)
            ?: session.cfirProvider.getClassIdBySymbol(symbol)
    }

    private fun resolveEnumConstructorOwnerClassId(symbol: CfirEnumConstructorSymbol): org.cangnova.cangjie.name.ClassId? {
        return session.symbolProvider.getEnumConstructorOwnerClassId(symbol)
            ?: session.cfirProvider.getEnumConstructorOwnerClassId(symbol)
    }

    private fun extractFunctionLikeType(type: ConeCangJieType?): ConeFuncType? {
        return type as? ConeFuncType
    }

    private fun functionLikeTypeFromFunction(function: CfirFunction): ConeFuncType {
        val parameterTypes = function.valueParameters.mapIndexed { index, parameter ->
            val typeRef = parameter.returnTypeRef as? CfirResolvedTypeRef
            typeRef?.coneType ?: ConeErrorType("unresolved parameter type at index $index")
        }
        val returnType = (function.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            ?: ConeErrorType("unresolved return type")
        return ConeFuncType(parameterTypes, returnType)
    }
    private fun extractReturnTypeFromSymbol(symbol: CfirSymbol<*>): ConeCangJieType {
        if (symbol is CfirFunctionSymbol && symbol.isBound) {
            val typeRef = symbol.cfir.returnTypeRef
            return if (typeRef is CfirResolvedTypeRef) typeRef.coneType
            else ConeErrorType("unresolved return type")
        }
        return extractTypeFromSymbol(symbol)
    }

    // ---- For-In ----

    override fun transformForInExpression(
        forInExpression: CfirForInExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        forInExpression.iterable.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)

        val iterableType = forInExpression.iterable.coneTypeOrNull
        val iterVarType = inferIterableElementType(iterableType)

        val varDecl = forInExpression.variable
        if (varDecl.returnTypeRef !is CfirResolvedTypeRef) {
            varDecl.replaceReturnTypeRef(
                buildResolvedTypeRef {
                    source = varDecl.returnTypeRef.source
                    delegatedTypeRef = varDecl.returnTypeRef
                    coneType = iterVarType
                }
            )
        }

        // Body typing is intentionally deferred in current flow.

        // `for-in` expression type is Unit.
        forInExpression.replaceConeTypeOrNull(builtinTypes.unitType)
        return forInExpression
    }

    /** Infer for-in element type from iterable type. */
    private fun inferIterableElementType(iterableType: ConeCangJieType?): ConeCangJieType {
        if (iterableType == null) return ConeErrorType("iterable has no type")

        if (iterableType is ConeClassLikeType && iterableType.classId == StdlibClassIds.Range) {
            return iterableType.typeArguments.firstOrNull() ?: ConePrimitiveType.INT64
        }
        if (iterableType is ConeStructType && iterableType.classId == StdlibClassIds.Range) {
            return iterableType.typeArguments.firstOrNull() ?: ConePrimitiveType.INT64
        }

        if (iterableType is ConeClassLikeType) {
            val typeArgs = iterableType.typeArguments
            if (typeArgs.isNotEmpty()) return typeArgs.first()
        }

        return ConeErrorType("cannot infer element type from: $iterableType")
    }

    // ---- Loop / While ----

    override fun transformLoopExpression(
        loopExpression: CfirLoopExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        // Resolve loop body and condition/parts.
        loopExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // loop/while expression type is Unit.
        loopExpression.replaceConeTypeOrNull(builtinTypes.unitType)
        return loopExpression
    }

    // ---- Throw ----

    override fun transformThrowExpression(
        throwExpression: CfirThrowExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        throwExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // `throw` expression type is Nothing.
        throwExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        return throwExpression
    }

    // ---- Try/Catch ----

    override fun transformTryExpression(
        tryExpression: CfirTryExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        tryExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        val branchTypes = mutableListOf<ConeCangJieType>()
        tryExpression.tryBlock.coneTypeOrNull?.let { branchTypes += it }
        for (catch in tryExpression.catches) {
            catch.body.coneTypeOrNull?.let { branchTypes += it }
        }

        val resultType = when {
            branchTypes.isEmpty() -> builtinTypes.unitType
            branchTypes.size == 1 -> branchTypes.first()
            else -> commonSupertype(branchTypes)
        }
        tryExpression.replaceConeTypeOrNull(resultType)
        return tryExpression
    }

    /**
     * Compute a practical common supertype (LUB-like). Falls back to union.
     */
    private fun commonSupertype(types: List<ConeCangJieType>): ConeCangJieType {
        if (types.isEmpty()) return builtinTypes.unitType
        val first = types.first()
        if (types.all { it == first }) return first

        val nonNothingTypes = types.filter { it != ConePrimitiveType.NOTHING }
        if (nonNothingTypes.isEmpty()) return ConePrimitiveType.NOTHING
        if (nonNothingTypes.size == 1) return nonNothingTypes.first()

        val typeCheckerContext = CfirTypeCheckerContext(session)

        // Build supertype chains for intersection by constructor.
        val supertypeChains = nonNothingTypes.map { type ->
            collectSupertypeChain(type, typeCheckerContext)
        }

        val commonTypes = supertypeChains.reduce { acc, chain ->
            acc.filter { candidate ->
                chain.any { typeCheckerContext.isSameTypeConstructor(candidate, it) }
            }
        }

        if (commonTypes.isEmpty()) {
            // No shared constructor-level supertype, return union.
            return ConeUnionType(types.toSet())
        }

        // Prefer the most specific candidate among common types.
        return commonTypes.firstOrNull { candidate ->
            commonTypes.none { other ->
                !typeCheckerContext.isSameTypeConstructor(candidate, other) &&
                    collectSupertypeChain(other, typeCheckerContext).any {
                        typeCheckerContext.isSameTypeConstructor(it, candidate)
                    }
            }
        } ?: commonTypes.first()
    }

    /** Collect supertypes by BFS including the type itself. */
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
            result.add(current)
            for (supertype in context.supertypes(current)) {
                if (visited.add(supertype)) {
                    queue.add(supertype)
                }
            }
        }
        return result
    }

    // ---- Subscript ----

    override fun transformSubscriptExpression(
        subscriptExpression: CfirSubscriptExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        subscriptExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        val resultType = when (val receiverType = subscriptExpression.receiver.coneTypeOrNull) {
            is ConeTupleType -> {
                val index = subscriptExpression.indices.firstOrNull()
                val indexValue = extractConstantIntIndex(index)
                if (indexValue != null && indexValue >= 0 && indexValue < receiverType.elementTypes.size) {
                    receiverType.elementTypes[indexValue]
                } else {
                    ConeErrorType("tuple index out of bounds or non-constant")
                }
            }
            is ConeVArrayType -> receiverType.elementType
            is ConeArrayType -> receiverType.elementType
            else -> {
                if (receiverType != null) {
                    val opName = Name.identifier("[]")
                    val argTypes = subscriptExpression.indices.mapNotNull { it.coneTypeOrNull }
                    CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(opName, receiverType, argTypes)
                        ?: ConeErrorType("no subscript operator for: $receiverType")
                } else {
                    ConeErrorType("receiver has no type")
                }
            }
        }
        subscriptExpression.replaceConeTypeOrNull(resultType)
        return subscriptExpression
    }

    /** Extract constant tuple index when literal int is provided. */
    private fun extractConstantIntIndex(expr: CfirExpression?): Int? {
        if (expr !is CfirLiteralExpression) return null
        if (expr.kind != CfirLiteralKind.INT) return null
        return (expr.value as? Long)?.toInt() ?: (expr.value as? Int)
    }

    // ---- Lambda ----

    override fun transformLambdaExpression(
        lambdaExpression: CfirLambdaExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        val anonFunc = lambdaExpression.anonymousFunction
        val expectedType = (data as? CfirResolutionMode.WithExpectedType)?.expectedType as? ConeFuncType

        val savedContext = context.towerDataContext
        val lambdaScope = CfirLocalScopeImpl()
        context.addLocalScope(lambdaScope)

        anonFunc.valueParameters.forEachIndexed { index, param ->
            if (param.returnTypeRef !is CfirResolvedTypeRef) {
                val expectedParamType = expectedType?.parameterTypes?.getOrNull(index)
                if (expectedParamType != null) {
                    param.replaceReturnTypeRef(
                        buildResolvedTypeRef {
                            source = param.returnTypeRef.source
                            delegatedTypeRef = param.returnTypeRef
                            coneType = expectedParamType
                        },
                    )
                }
            }
            val paramSymbol = param.symbol as? CfirCallableSymbol<*> ?: return@forEachIndexed
            lambdaScope.addVariable(param.name, paramSymbol)
        }

        try {
            anonFunc.body?.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
        } finally {
            context.withTowerDataContext(savedContext) {}
        }

        val paramTypes = anonFunc.valueParameters.mapIndexed { i, param ->
            val explicit = param.returnTypeRef
            if (explicit is CfirResolvedTypeRef) {
                explicit.coneType
            } else {
                expectedType?.parameterTypes?.getOrNull(i) ?: ConeErrorType("cannot infer lambda param type at $i")
            }
        }

        val returnType = when {
            expectedType != null -> expectedType.returnType
            anonFunc.returnTypeRef is CfirResolvedTypeRef -> (anonFunc.returnTypeRef as CfirResolvedTypeRef).coneType
            else -> anonFunc.body?.coneTypeOrNull ?: ConeErrorType("cannot infer lambda return type")
        }

        lambdaExpression.replaceConeTypeOrNull(ConeFuncType(paramTypes, returnType))
        return lambdaExpression
    }

    // ---- Range ----

    override fun transformRangeExpression(
        rangeExpression: CfirRangeExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        rangeExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // Range element type is inferred from start/end, defaulting to Int64.
        val startType = rangeExpression.start.coneTypeOrNull
        val endType = rangeExpression.end.coneTypeOrNull
        val elementType = when {
            startType != null && startType == endType -> IdealTypeResolver.resolveIfIdeal(startType, null)
            startType != null -> IdealTypeResolver.resolveIfIdeal(startType, null)
            else -> ConePrimitiveType.INT64
        }

        rangeExpression.replaceConeTypeOrNull(
            ConeStructType(ConeClassLookupTagImpl(StdlibClassIds.Range), listOf(elementType))
        )
        return rangeExpression
    }

    // ---- Spawn ----

    override fun transformSpawnExpression(
        spawnExpression: CfirSpawnExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        spawnExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // Spawn returns Future<BodyType>.
        val taskReturnType = spawnExpression.body.coneTypeOrNull ?: builtinTypes.unitType
        spawnExpression.replaceConeTypeOrNull(
            ConeClassLikeType(ConeClassLookupTagImpl(StdlibClassIds.Future), listOf(taskReturnType))
        )
        return spawnExpression
    }

    /**
     * Re-resolve lambda arguments after candidate selection with substituted param types.
     */
    private fun retransformLambdaArguments(
        functionCall: CfirFunctionCall,
        candidate: CfirCandidate,
    ) {
        if (!candidate.symbol.isBound) return
        for ((argumentAtom, parameter) in candidate.argumentMapping) {
            val arg = argumentAtom.expression
            if (arg !is CfirLambdaExpression) continue
            val rawParamType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            val paramType = candidate.substitutor.substituteOrSelf(rawParamType)
            if (paramType !is ConeFuncType) continue

            // Re-transform lambda with precise expected function type.
            arg.transform<CfirElement, CfirResolutionMode>(
                transformer,
                CfirResolutionMode.WithExpectedType(paramType),
            )
        }
    }
}
