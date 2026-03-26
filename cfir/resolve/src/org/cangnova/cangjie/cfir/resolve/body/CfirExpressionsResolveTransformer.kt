package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildQualifiedAccess
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedNamedReferenceImpl
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.typeFromCallee
import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassStaticScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

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
@OptIn(CfirImplementationDetail::class)
class CfirExpressionsResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
) : CfirPartialBodyResolveTransformer(transformer) {

    private val builtinTypes get() = session.builtinTypes
    private val callResolver get() = components.callResolver
    private val towerResolver get() = components.towerResolver

    private fun errorType(
        reason: String,
        kind: DiagnosticKind = DiagnosticKind.Other,
        delegatedType: ConeCangJieType? = null,
    ): ConeErrorType = ConeErrorType(ConeSimpleDiagnostic(reason, kind), delegatedType = delegatedType)

    init {
        components.callResolver.initTransformer(this)
    }

    // ── Literals ─────────────────────────────────────────────────────────────

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

    // ── Property Access ───────────────────────────────────────────────────────

    override fun transformPropertyAccess(
        propertyAccess: CfirPropertyAccess,
        data: ResolutionMode,
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
            propertyAccess.replaceConeTypeOrNull(
                ConeErrorType(ConeSimpleDiagnostic("non-name reference", DiagnosticKind.Other))
            )
            return propertyAccess
        }

        val resolvedAccess = callResolver.resolveVariableAccessAndSelectCandidate(propertyAccess, data)
        return completeResolvedAccess(resolvedAccess, data)
    }

    // ── Qualified Access ──────────────────────────────────────────────────────

    override fun transformQualifiedAccess(
        qualifiedAccess: CfirQualifiedAccess,
        data: ResolutionMode,
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
            qualifiedAccess.replaceConeTypeOrNull(
                ConeErrorType(ConeSimpleDiagnostic("non-name reference", DiagnosticKind.Other))
            )
            return qualifiedAccess
        }

        val resolvedAccess = callResolver.resolveVariableAccessAndSelectCandidate(qualifiedAccess, data)
        return completeResolvedAccess(resolvedAccess, data)
    }

    // ── Function Call ─────────────────────────────────────────────────────────

    override fun transformFunctionCall(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirExpression {
        functionCall.transformChildren(transformer, ResolutionMode.ContextIndependent)

        val reference = functionCall.calleeReference

        if (reference is CfirResolvedNamedReference) {
            val appliedReturnType = (reference as? CfirResolvedAppliedCallableReference)?.substitutedReturnType
            functionCall.replaceConeTypeOrNull(
                appliedReturnType ?: extractReturnTypeFromSymbol(reference.resolvedSymbol)
            )
            return functionCall
        }

        if (reference !is CfirNamedReference) {
            functionCall.replaceConeTypeOrNull(
                ConeErrorType(ConeSimpleDiagnostic("non-name callee reference", DiagnosticKind.Other))
            )
            return functionCall
        }

        val resolvedCall = callResolver.resolveCallAndSelectCandidate(functionCall, data)
        val candidateReference = resolvedCall.calleeReference as? CfirNamedReferenceWithCandidate
        if (candidateReference != null) {
            retransformLambdaArguments(resolvedCall, candidateReference.candidate)
            return components.callCompleter.completeCall(resolvedCall, data)
        }

        if (resolvedCall.calleeReference is CfirResolvedNamedReference) {
            if (resolvedCall.coneTypeOrNull == null) {
                resolvedCall.replaceConeTypeOrNull(components.typeFromCallee(resolvedCall))
            }
            return resolvedCall
        }

        resolveCallFallbacks(resolvedCall, reference, data)
        return resolvedCall
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
        data: ResolutionMode,
    ) {
        if (tryEnumConstructorFallback(functionCall, reference, data)) return
        if (tryCallableVariableInvokeFallback(functionCall, reference)) return

        val builtinType = tryBuiltinOperatorFallback(functionCall, reference)?.returnType
        functionCall.replaceConeTypeOrNull(
            builtinType ?: ConeErrorType(ConeUnresolvedNameError(reference.name))
        )
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
        data: ResolutionMode,
    ): CfirExpression {
        ifExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
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
        val leftType  = comparisonExpression.left.coneTypeOrNull
        val rightType = comparisonExpression.right.coneTypeOrNull
        val resultType = if (leftType != null && rightType != null) {
            CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
                Name.identifier(comparisonExpression.operation.toFunctionName()),
                leftType,
                listOf(rightType),
            )?.returnType ?: builtinTypes.boolType
        } else {
            builtinTypes.boolType
        }
        comparisonExpression.replaceConeTypeOrNull(resultType)
        return comparisonExpression
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
                if (arrayElementType != null) {
                    arrayElementType
                } else if (receiverType != null) {
                    val argTypes = subscriptExpression.indices.mapNotNull { it.coneTypeOrNull }
                    CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
                        Name.identifier("[]"), receiverType, argTypes
                    )?.returnType ?: errorType("no subscript operator for: $receiverType")
                } else {
                    errorType("receiver has no type")
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

    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: CfirAnonymousFunctionExpression,
        data: ResolutionMode,
    ): CfirExpression {
        val anonFunc = anonymousFunctionExpression.anonymousFunction
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
                    ?: errorType("cannot infer lambda param type at $i")
            }
        }) { anonFunc.body?.resolveIndependently() }

        val returnType = when {
            expectedFuncType != null                          -> expectedFuncType.returnType
            anonFunc.returnTypeRef is CfirResolvedTypeRef     -> (anonFunc.returnTypeRef as CfirResolvedTypeRef).coneType
            else                                              -> anonFunc.body?.coneTypeOrNull
                ?: errorType("cannot infer lambda return type")
        }

        anonymousFunctionExpression.replaceConeTypeOrNull(ConeFuncType(paramTypes, returnType))
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

    // ── Fallback Helpers ──────────────────────────────────────────────────────

    private fun tryEnumConstructorFallback(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
        data: ResolutionMode,
    ): Boolean {
        val resolvedCall = callResolver.resolveVariableAccessAndSelectCandidate(
            qualifiedAccess = functionCall,
            resolutionMode = data,
            forceCallKind = CallKind.EnumConstructorCall,
        )
        val candidateReference = resolvedCall.calleeReference as? CfirNamedReferenceWithCandidate ?: return false
        retransformLambdaArguments(resolvedCall, candidateReference.candidate)
        components.callCompleter.completeCall(resolvedCall, data)

        val expectedType = data.expectedTypeOrNull
        resolvedCall.replaceConeTypeOrNull(
            resolvedCall.coneTypeOrNull?.let { refineResolvedCallType(it, expectedType) }
                ?: ConeErrorType(ConeSimpleDiagnostic("unresolved return type", DiagnosticKind.Other))
        )
        return true
    }

    private fun <T> completeResolvedAccess(
        access: T,
        data: ResolutionMode,
    ): T where T : CfirExpression, T : org.cangnova.cangjie.cfir.CfirResolvable {
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

    private fun tryBuiltinOperatorFallback(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
    ): BuiltinPrimitiveOperatorMatch? {
        val receiverType = functionCall.explicitReceiver?.coneTypeOrNull
        val argTypes = functionCall.arguments.mapNotNull { it.coneTypeOrNull }
        val match = CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(reference.name, receiverType, argTypes)
            ?: return null

        val receiverScope = receiverType?.let(::getMemberScope) ?: return match
        val symbol = resolveBuiltinOperatorSymbol(receiverScope, match.signature) ?: return match
        functionCall.replaceCalleeReference(
            CfirResolvedAppliedCallableReference(
                source = null,
                name = reference.name,
                resolvedSymbol = symbol,
                substitutedReturnType = match.returnType,
                substitutedParameterTypes = match.signature.parameterKinds.map(::ConePrimitiveType),
            )
        )
        functionCall.replaceConeTypeOrNull(match.returnType)
        return match
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
            if (arg is CfirAnonymousFunctionExpression) {
                arg.transform<CfirElement, ResolutionMode>(
                    transformer,
                    ResolutionMode.WithExpectedType(buildResolvedTypeRef { coneType = expectedParamType }),
                )
            }
        }
        functionCall.replaceConeTypeOrNull(functionType.returnType)
        return true
    }

    // ── Member Scope Helpers ──────────────────────────────────────────────────

    private fun resolveWithReceiver(name: Name, receiver: CfirExpression): ConeCangJieType {
        val receiverType = receiver.coneTypeOrNull
            ?: return errorType("receiver has no type")
        val memberScope = receiver.resolvedQualifierClassifier(session)?.cfir?.let(::CfirClassStaticScope)
            ?: getMemberScope(receiverType)
            ?: return errorType("no member scope for type: $receiverType")

        val candidates = mutableListOf<CfirCallableSymbol<*>>()
        memberScope.processCallablesByName(name) { candidates += it }
        memberScope.processPropertiesByName(name) { candidates += it }
        memberScope.processFunctionsByName(name) { candidates += it }

        if (candidates.isNotEmpty()) {
            return extractTypeFromCallableSymbol(candidates.first())
        }

        val classifiers = mutableListOf<CfirClassLikeSymbol<*>>()
        memberScope.processClassifiersByName(name) { classifiers += it }
        return if (classifiers.isEmpty()) {
            ConeErrorType(ConeUnresolvedNameError(name, receiverType = receiverType))
        } else {
            extractTypeFromSymbol(classifiers.first())
        }
    }

    private fun getMemberScope(type: ConeCangJieType): CfirClassUseSiteMemberScope? {
        val classId = type.classIdOrPrimitiveClassId ?: return null
        val classSymbol = components.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        return CfirClassUseSiteMemberScope(classSymbol, components.symbolProvider, session.extendProvider)
    }

    private fun resolveBuiltinOperatorSymbol(
        scope: CfirClassUseSiteMemberScope,
        signature: BuiltinPrimitiveOperatorSignature,
    ): CfirFunctionSymbol<*>? {
        val ownerClassId = signature.receiverKind.classId
        val candidates = mutableListOf<CfirFunctionSymbol<*>>()
        scope.processFunctionsByName(signature.name) { candidates += it }
        return candidates.firstOrNull { symbol ->
            session.symbolProvider.getContainingClassId(symbol) == ownerClassId &&
                symbolMatchesBuiltinSignature(symbol, signature)
        }
    }

    private fun symbolMatchesBuiltinSignature(
        symbol: CfirFunctionSymbol<*>,
        signature: BuiltinPrimitiveOperatorSignature,
    ): Boolean {
        val declaration = symbol.cfir
        if (declaration.valueParameters.size != signature.parameterKinds.size) return false
        val returnType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType as? ConePrimitiveType ?: return false
        if (returnType.kind != signature.returnKind) return false
        return declaration.valueParameters.mapNotNull { parameter ->
            ((parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType as? ConePrimitiveType)?.kind
        } == signature.parameterKinds
    }

    // ── Type Extraction ───────────────────────────────────────────────────────

    private fun extractTypeFromCallableSymbol(symbol: CfirCallableSymbol<*>): ConeCangJieType {
        if (!symbol.isBound) return errorType("unbound symbol")
        return when (val decl = symbol.cfir) {
            is CfirFunction        -> functionLikeTypeFromFunction(decl)
            is CfirProperty        -> resolvedConeTypeOf(decl.returnTypeRef, symbol)
            is CfirFieldVariable   -> resolvedConeTypeOf(decl.returnTypeRef, symbol)
            is CfirPatternVariable -> resolvedConeTypeOf(decl.returnTypeRef, symbol)
            is CfirValueParameter  -> resolvedConeTypeOf(decl.returnTypeRef, symbol)
            is CfirEnumConstructor -> resolveEnumConstructorOwnerType(symbol as? CfirEnumConstructorSymbol)
                ?.coneType ?: resolvedConeTypeOf(decl.returnTypeRef, symbol)
            else -> errorType("unsupported callable declaration: ${decl::class.simpleName}")
        }
    }

    private fun resolvedConeTypeOf(typeRef: CfirTypeRef, symbol: CfirSymbol<*>): ConeCangJieType =
        if (typeRef is CfirResolvedTypeRef) typeRef.coneType
        else errorType("unresolved type for ${symbol::class.simpleName}")

    private fun extractTypeFromSymbol(symbol: CfirSymbol<*>): ConeCangJieType = when (symbol) {
        is CfirCallableSymbol<*> -> extractTypeFromCallableSymbol(symbol)
        is CfirClassLikeSymbol<*> -> {
            val classId = resolveClassIdBySymbol(symbol)
                ?: return errorType("unresolved class id for symbol: ${symbol.debugName}")
            val typeArgs = typeParametersOf(symbol.cfir).map {
                ConeTypeProjection(ConeTypeParameterTypeImpl(it.symbol.toLookupTag()))
            }
            constructClassLikeType(symbol, classId, typeArgs)
        }
        else -> errorType("unsupported symbol type: ${symbol::class.simpleName}")
    }

    private fun extractTypeFromSymbolWithExpected(
        symbol: CfirSymbol<*>,
        data: ResolutionMode,
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
        if (symbol is CfirFunctionSymbol<*> && symbol.isBound) {
            val typeRef = symbol.cfir.returnTypeRef
            return if (typeRef is CfirResolvedTypeRef) typeRef.coneType
            else errorType("unresolved return type")
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
        val ownerSymbol = components.symbolProvider.getClassLikeSymbolByClassId(classId)
        val typeArgs = ownerSymbol?.let { typeParametersOf(it.cfir) }?.map {
            ConeTypeProjection(ConeTypeParameterTypeImpl(it.symbol.toLookupTag()))
        }.orEmpty()
        val ownerType = ownerSymbol?.let {
            constructClassLikeType(it, classId, typeArgs)
        } ?: ConeEnumType(classId.toLookupTag(), typeArgs)
        return buildResolvedTypeRef {
            source = symbol.cfir.returnTypeRef.source
            delegatedTypeRef = symbol.cfir.returnTypeRef
            coneType = ownerType
        }
    }

    private fun functionLikeTypeFromFunction(function: CfirFunction): ConeFuncType {
        val parameterTypes = function.valueParameters.mapIndexed { i, param ->
            (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                ?: errorType("unresolved parameter type at index $i")
        }
        val returnType = (function.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            ?: errorType("unresolved return type")
        return ConeFuncType(parameterTypes, returnType)
    }

    private fun extractFunctionLikeType(type: ConeCangJieType?): ConeFuncType? = type as? ConeFuncType

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
        is CfirPrimitiveTypeSymbol -> ConePrimitiveType(symbol.kind)
        is CfirInterfaceSymbol -> ConeClassLikeType(classId.toLookupTag(), typeArguments, isInterface = true)
        is CfirStructSymbol -> ConeStructType(classId.toLookupTag(), typeArguments)
        is CfirEnumSymbol -> ConeEnumType(classId.toLookupTag(), typeArguments, isRefEnum = symbol.isRefEnum)
        is CfirTypeAliasSymbol -> ConeTypeAliasType(classId, typeArguments = typeArguments)
        else -> ConeClassLikeType(classId.toLookupTag(), typeArguments)
    }

    private fun typeParametersOf(declaration: CfirDeclaration): List<CfirTypeParameter> = when (declaration) {
        is CfirPrimitiveTypeDeclaration -> emptyList()
        is CfirClass -> declaration.typeParameters
        is CfirInterface -> declaration.typeParameters
        is CfirStruct -> declaration.typeParameters
        is CfirEnum -> declaration.typeParameters
        is CfirTypeAlias -> declaration.typeParameters
        else -> emptyList()
    }

    private fun resolveClassIdBySymbol(symbol: CfirClassLikeSymbol<*>): org.cangnova.cangjie.name.ClassId? =
        symbol.classId.takeUnless { it.asString().isEmpty() }

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
    private fun retransformLambdaArguments(functionCall: CfirFunctionCall, candidate: Candidate) {
        if (!candidate.symbol.isBound || !candidate.argumentMappingInitialized) return
        for ((argumentAtom, parameter) in candidate.argumentMapping) {
            val arg = argumentAtom.expression as? CfirAnonymousFunctionExpression ?: continue
            val rawParamType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            val paramType = candidate.substitutor.substituteOrSelf(rawParamType) as? ConeFuncType ?: continue
            arg.transform<CfirElement, ResolutionMode>(
                transformer,
                ResolutionMode.WithExpectedType(buildResolvedTypeRef { coneType = paramType }),
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

    // ── Small Extension Utilities ─────────────────────────────────────────────

    private fun CfirExpression.resolveIndependently() {
        transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
    }

    private fun CfirExpression.resolveIndependently(body: CfirBlock?) {
        body?.transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
    }

    private val ResolutionMode.expectedTypeOrNull: ConeCangJieType?
        get() = (this as? ResolutionMode.WithExpectedType)?.expectedTypeRef?.coneType

    /**
     * Shared factory for [CallInfo] to reduce repetition at call sites.
     */
    private fun buildCallInfo(
        callSite: CfirExpression,
        kind: CallKind,
        name: Name,
        explicitReceiver: CfirExpression?,
        arguments: List<CfirExpression>,
        typeArguments: List<CfirTypeRef>,
    ) = CallInfo(
        callSite = callSite,
        callKind = kind,
        name = name,
        explicitReceiver = explicitReceiver,
        arguments = arguments,
        isUsedAsGetClassReceiver = false,
        typeArguments = typeArguments,
        session = session,
        containingFile = components.file,
        containingDeclarations = components.containingDeclarations,
        resolutionMode = ResolutionMode.ContextIndependent,
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
        @Suppress("UNUSED_PARAMETER") data: ResolutionMode,
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
