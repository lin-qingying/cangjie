package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedNamedReferenceImpl
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCallKind
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckArguments
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckVisibility
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirInferTypeArguments
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirMapArguments
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.Name

/**
 * 表达式类型合成 transformer。
 *
 * 负责为每种表达式节点合成（synthesize）类型：
 * - 字面量 -> 直接映射 ConeCangjieType
 * - 变量引用 -> scope 查找 -> 绑定符号 -> 提取类型
 * - 属性访问 -> 接收者类型 member scope 查找
 * - 函数调用 -> call resolver -> 返回类型
 * - 复合表达式 -> 递归合成 + 组合规则
 *
 * 参考 K2 FirExpressionsResolveTransformer。
 */
@OptIn(CfirImplementationDetail::class)
class CfirExpressionsResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
) : CfirPartialBodyResolveTransformer(transformer) {

    private val builtinTypes get() = session.builtinTypes
    private val callResolver get() = components.callResolver
    private val towerResolver get() = components.towerResolver

    // ---- 字面量 ----

    override fun transformLiteralExpression(
        literalExpression: CfirLiteralExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        val synthesized = synthesizeLiteralType(literalExpression.kind)
        // Phase 6: 当有期望类型时，将 IdealType 具体化为具体类型
        val expectedType = (data as? CfirResolutionMode.WithExpectedType)?.expectedType
        val type = IdealTypeResolver.resolveIfIdeal(synthesized, expectedType)
        literalExpression.replaceConeTypeOrNull(type)
        return literalExpression
    }

    private fun synthesizeLiteralType(kind: CfirLiteralKind): ConeCangjieType {
        return when (kind) {
            CfirLiteralKind.INT -> ConePrimitiveType.IDEAL_INT
            CfirLiteralKind.FLOAT -> ConePrimitiveType.IDEAL_FLOAT
            CfirLiteralKind.BOOLEAN -> builtinTypes.boolType
            CfirLiteralKind.RUNE -> ConePrimitiveType.RUNE
            CfirLiteralKind.STRING -> ConeClassLikeType(ConeClassLookupTagImpl(StdlibClassIds.String))
            CfirLiteralKind.UNIT -> builtinTypes.unitType
            CfirLiteralKind.NULL -> ConePrimitiveType.NOTHING
        }
    }

    // ---- 属性访问 / 变量引用 ----

    override fun transformPropertyAccess(
        propertyAccess: CfirPropertyAccess,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 先递归解析显式接收者
        val receiver = propertyAccess.explicitReceiver
        receiver?.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)

        val reference = propertyAccess.calleeReference
        if (reference is CfirResolvedNamedReference) {
            // 已解析引用：直接从符号提取类型
            propertyAccess.replaceConeTypeOrNull(extractTypeFromSymbol(reference.resolvedSymbol))
            return propertyAccess
        }

        if (reference !is CfirNamedReference) {
            propertyAccess.replaceConeTypeOrNull(ConeErrorType("non-name reference"))
            return propertyAccess
        }

        val name = reference.name

        if (receiver != null) {
            // 有接收者：在接收者类型的成员 scope 中查找
            val resolvedType = resolveWithReceiver(name, receiver)
            propertyAccess.replaceConeTypeOrNull(resolvedType)
        } else {
            // 无接收者：在 scope 塔中查找
            val candidates = towerResolver.findVariables(name)
            if (candidates.isEmpty()) {
                propertyAccess.replaceConeTypeOrNull(ConeErrorType(ConeUnresolvedReferenceError(name)))
            } else {
                val symbol = candidates.first()
                // 绑定解析后的引用
                (propertyAccess as? org.cangnova.cangjie.cfir.expressions.impl.CfirPropertyAccessImpl)?.calleeReference =
                    CfirResolvedNamedReferenceImpl(null, name, symbol)
                propertyAccess.replaceConeTypeOrNull(extractTypeFromCallableSymbol(symbol))
            }
        }
        return propertyAccess
    }

    // ---- 限定访问 ----

    override fun transformQualifiedAccess(
        qualifiedAccess: CfirQualifiedAccess,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 先递归解析显式接收者
        val receiver = qualifiedAccess.explicitReceiver
        receiver?.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)

        val reference = qualifiedAccess.calleeReference
        if (reference is CfirResolvedNamedReference) {
            qualifiedAccess.replaceConeTypeOrNull(extractTypeFromSymbol(reference.resolvedSymbol))
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
            if (candidates.isEmpty()) {
                qualifiedAccess.replaceConeTypeOrNull(ConeErrorType(ConeUnresolvedReferenceError(name)))
            } else {
                val symbol = candidates.first()
                (qualifiedAccess as? org.cangnova.cangjie.cfir.expressions.impl.CfirQualifiedAccessImpl)?.calleeReference =
                    CfirResolvedNamedReferenceImpl(null, name, symbol)
                qualifiedAccess.replaceConeTypeOrNull(extractTypeFromCallableSymbol(symbol))
            }
        }
        return qualifiedAccess
    }

    // ---- 函数调用 ----

    override fun transformFunctionCall(
        functionCall: CfirFunctionCall,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 先递归变换子表达式
        functionCall.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        val reference = functionCall.calleeReference
        if (reference is CfirResolvedNamedReference) {
            functionCall.replaceConeTypeOrNull(extractReturnTypeFromSymbol(reference.resolvedSymbol))
            return functionCall
        }

        if (reference !is CfirNamedReference) {
            functionCall.replaceConeTypeOrNull(ConeErrorType("non-name callee reference"))
            return functionCall
        }

        // Phase 3: 尝试完整的调用解析流程
        val resolutionContext = components.resolutionContext
        if (resolutionContext != null) {
            return resolveCallWithPhase3(functionCall, reference, resolutionContext)
        }

        // 回退到旧版解析
        return resolveCallLegacy(functionCall, reference)
    }

    /**
     * Phase 3 完整调用解析：构建 CallInfo -> 调用解析 -> 绑定结果。
     */
    private fun resolveCallWithPhase3(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
        resolutionContext: org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext,
    ): CfirExpression {
        val callInfo = CfirCallInfo(
            callSite = functionCall,
            callKind = CfirCallKind.Function(
                listOf(CfirCheckVisibility, CfirMapArguments, CfirInferTypeArguments, CfirCheckArguments)
            ),
            name = reference.name,
            explicitReceiver = functionCall.explicitReceiver,
            arguments = functionCall.arguments,
            typeArguments = functionCall.typeArguments,
            session = session,
        )

        when (val result = callResolver.resolveCallAndSelectCandidate(callInfo, resolutionContext)) {
            is CfirCallResolutionResult.Success -> {
                val candidate = result.candidate
                (functionCall as? org.cangnova.cangjie.cfir.expressions.impl.CfirFunctionCallImpl)?.calleeReference =
                    CfirResolvedNamedReferenceImpl(null, reference.name, candidate.symbol)
                val returnType = candidate.resolvedReturnType() ?: ConeErrorType("unresolved return type")
                functionCall.replaceConeTypeOrNull(returnType)
            }
            is CfirCallResolutionResult.ResolvedWithErrors -> {
                val candidate = result.candidate
                (functionCall as? org.cangnova.cangjie.cfir.expressions.impl.CfirFunctionCallImpl)?.calleeReference =
                    CfirResolvedNamedReferenceImpl(null, reference.name, candidate.symbol)
                val returnType = candidate.resolvedReturnType() ?: ConeErrorType("resolved with errors")
                functionCall.replaceConeTypeOrNull(returnType)
            }
            is CfirCallResolutionResult.Ambiguity -> {
                functionCall.replaceConeTypeOrNull(ConeErrorType("ambiguous call: ${reference.name}"))
            }
            is CfirCallResolutionResult.NoCandidate -> {
                // Phase 5: 尝试内建操作符回退
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

    /**
     * 旧版调用解析回退路径。
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
                functionCall.replaceConeTypeOrNull(ConeErrorType(ConeUnresolvedNameError(reference.name)))
            }
            else -> {
                functionCall.replaceConeTypeOrNull(ConeErrorType("unexpected resolution result"))
            }
        }
        return functionCall
    }

    // ---- 块表达式 ----

    override fun transformBlock(block: CfirBlock, data: CfirResolutionMode): CfirExpression {
        // 递归解析所有语句
        block.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // 块的类型 = 最后一个表达式的类型，否则为 Unit
        val lastExpr = block.statements.lastOrNull()
        val blockType = if (lastExpr is CfirExpression) {
            lastExpr.coneTypeOrNull ?: builtinTypes.unitType
        } else {
            builtinTypes.unitType
        }
        block.replaceConeTypeOrNull(blockType)
        return block
    }

    // ---- match 表达式 ----

    /**
     * match 表达式类型合成（Phase 4）。
     *
     * 1. 解析 subject 表达式 -> 获得 subjectType
     * 2. 遍历每个 branch:
     *    a. 解析 pattern（类型检查 pattern 与 subjectType 的兼容性）
     *    b. 若有 guard -> 解析 guard（应为 Boolean 类型）
     *    c. 解析 body -> 获得 branchType
     * 3. match 表达式的类型 = 所有 branchType 的公共超类型（LUB）
     */
    override fun transformMatchExpression(
        matchExpression: CfirMatchExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 递归变换 subject 和所有 branches
        matchExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        val subjectType = matchExpression.subject.coneTypeOrNull

        // 收集所有分支的结果类型
        val branchTypes = matchExpression.branches.map { branch ->
            // 解析 guard（期望 Bool）
            if (branch.guard != null) {
                branch.guard?.coneTypeOrNull
            }
            // 分支的结果类型
            branch.body.coneTypeOrNull ?: builtinTypes.unitType
        }

        // 计算 match 表达式的结果类型
        val resultType = computeMatchResultType(branchTypes)
        matchExpression.replaceConeTypeOrNull(resultType)
        return matchExpression
    }

    /** 解析单个 match 分支 */
    private fun resolveBranch(
        branch: CfirMatchBranch,
        subjectType: ConeCangjieType?,
        branchTypes: MutableList<ConeCangjieType>,
    ): CfirMatchBranch {
        // 解析模式
        resolvePattern(branch.pattern, subjectType)

        // 验证 guard 类型（如果存在）
        val guard = branch.guard
        if (guard != null) {
            val guardType = guard.coneTypeOrNull
            // TODO: Phase 5 强制检查 guard 为 Bool 类型
        }

        // 收集分支结果类型
        val bodyType = branch.body.coneTypeOrNull ?: builtinTypes.unitType
        branch.replaceConeTypeOrNull(bodyType)
        branchTypes.add(bodyType)

        return branch
    }

    /**
     * 解析模式，检查模式类型与期望类型的兼容性。
     */
    private fun resolvePattern(pattern: CfirPattern, expectedType: ConeCangjieType?) {
        when (pattern) {
            is CfirWildcardPattern -> {
                // 通配符模式总是匹配
            }
            is CfirConstPattern -> {
                // 解析常量表达式，检查类型兼容
                // 表达式由 transformChildren 在上层调用时已递归处理
            }
            is CfirBindingPattern -> {
                // 绑定变量类型 = pattern.typeRef 指定的类型，否则为 expectedType
                val bindingType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType
                    ?: expectedType

                // 将绑定变量加入当前局部 scope
                if (bindingType != null) {
                    storePatternBinding(pattern.name, bindingType)
                }

                // 递归解析嵌套模式
                pattern.nestedPattern?.let { nested -> resolvePattern(nested, expectedType) }
            }
            is CfirTuplePattern -> {
                // expectedType 应为 ConeTupleType，递归解析子模式
                val tupleType = expectedType as? ConeTupleType
                pattern.elements.forEachIndexed { index, subPattern ->
                    val elementType = tupleType?.elementTypes?.getOrNull(index)
                    resolvePattern(subPattern, elementType)
                }
            }
            is CfirEnumPattern -> {
                // 递归解析枚举构造器参数模式
                pattern.arguments.forEach { argPattern ->
                    resolvePattern(argPattern, null)
                }
            }
            is CfirTypePattern -> {
                // 类型模式：检查 pattern 类型与 expectedType 的子类型关系
                val patternType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType
                if (patternType != null && pattern.bindingName != null) {
                    storePatternBinding(pattern.bindingName!!, patternType)
                }
            }
        }
    }

    /** 将模式绑定的变量存储到当前局部 scope */
    private fun storePatternBinding(name: Name, type: ConeCangjieType) {
        val symbol = CfirVariableSymbol()
        buildVariable {
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

    /** 计算 match 表达式的结果类型（所有分支类型的 LUB） */
    private fun computeMatchResultType(branchTypes: List<ConeCangjieType>): ConeCangjieType {
        if (branchTypes.isEmpty()) return builtinTypes.unitType
        if (branchTypes.size == 1) return branchTypes.single()

        // 所有类型相同
        val first = branchTypes.first()
        if (branchTypes.all { it == first }) return first

        // 不同类型：使用 ConeUnionType 表示，后续由推断阶段细化
        return ConeUnionType(branchTypes.toSet())
    }

    // ---- if 表达式 ----

    override fun transformIfExpression(
        ifExpression: CfirIfExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 递归变换 condition、thenBranch、elseBranch
        ifExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // if 表达式的类型 = then 和 else 分支的公共超类型
        val thenType = ifExpression.thenBranch.coneTypeOrNull
        val elseType = ifExpression.elseBranch?.let { (it as? CfirExpression)?.coneTypeOrNull }

        val resultType = when {
            thenType == null -> elseType ?: builtinTypes.unitType
            elseType == null -> builtinTypes.unitType // 无 else -> Unit（语句用法）
            thenType == elseType -> thenType
            // Phase 2 简化：不同类型使用 ConeUnionType 表示，后续由推断阶段细化
            else -> ConeUnionType(setOf(thenType, elseType))
        }
        ifExpression.replaceConeTypeOrNull(resultType)
        return ifExpression
    }

    // ---- return 表达式 ----

    override fun transformReturnExpression(
        returnExpression: CfirReturnExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 递归变换 result（如果存在）
        returnExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // return 表达式的类型 = Nothing
        returnExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        return returnExpression
    }

    // ---- 赋值表达式 ----

    override fun transformAssignment(
        assignment: CfirAssignment,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 递归变换 lValue 和 rValue
        assignment.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // 赋值类型 = Unit
        assignment.replaceConeTypeOrNull(builtinTypes.unitType)
        return assignment
    }

    // ---- 元组字面量 ----

    override fun transformTupleLiteral(
        tupleLiteral: CfirTupleLiteral,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 递归变换所有元素
        tupleLiteral.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        val elementTypes = tupleLiteral.elements.map { it.coneTypeOrNull ?: ConeErrorType("unresolved element") }
        tupleLiteral.replaceConeTypeOrNull(ConeTupleType(elementTypes))
        return tupleLiteral
    }

    // ---- 数组字面量 ----

    override fun transformArrayLiteral(
        arrayLiteral: CfirArrayLiteral,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 递归变换所有元素
        arrayLiteral.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        val elementTypes = arrayLiteral.elements.mapNotNull { it.coneTypeOrNull }
        // 数组元素类型：Phase 2 取第一个元素类型，后续由推断阶段统一
        val elementType = elementTypes.firstOrNull() ?: ConeErrorType("empty array literal")
        arrayLiteral.replaceConeTypeOrNull(ConeArrayType(elementType))
        return arrayLiteral
    }

    // ---- 字符串插值 ----

    override fun transformStringInterpolation(
        stringInterpolation: CfirStringInterpolation,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 递归变换所有插值部分
        stringInterpolation.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        stringInterpolation.replaceConeTypeOrNull(
            ConeClassLikeType(ConeClassLookupTagImpl(StdlibClassIds.String))
        )
        return stringInterpolation
    }

    // ---- 比较表达式 ----

    override fun transformComparisonExpression(
        comparisonExpression: CfirComparisonExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 先递归变换子表达式（通过 dispatcher）
        comparisonExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        val leftType = comparisonExpression.left.coneTypeOrNull
        val rightType = comparisonExpression.right.coneTypeOrNull

        // 尝试内建比较操作符解析
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

        // TODO: 用户类型操作符重载（Equatable/Comparable 接口）
        // 回退：返回 Bool（保持向后兼容）
        comparisonExpression.replaceConeTypeOrNull(builtinTypes.boolType)
        return comparisonExpression
    }

    // ---- 逻辑 / 空合 / 管道操作符 ----

    override fun transformBinaryOp(
        binaryOp: CfirBinaryOp,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 递归变换左右操作数
        binaryOp.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // 根据操作符种类确定结果类型
        val resultType = when (binaryOp.kind) {
            CfirBinaryOpKind.AND, CfirBinaryOpKind.OR -> builtinTypes.boolType
            CfirBinaryOpKind.COALESCING -> binaryOp.left.coneTypeOrNull ?: ConeErrorType("unresolved coalescing left")
            CfirBinaryOpKind.PIPELINE -> binaryOp.right.coneTypeOrNull ?: ConeErrorType("unresolved pipeline right")
        }
        binaryOp.replaceConeTypeOrNull(resultType)
        return binaryOp
    }

    // ---- 类型操作符 ----

    override fun transformTypeOperator(
        typeOperator: CfirTypeOperator,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 递归变换 argument
        typeOperator.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // 根据 operation 确定结果类型
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

    // ---- 错误表达式 ----

    override fun transformErrorExpression(
        errorExpression: CfirErrorExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        errorExpression.replaceConeTypeOrNull(ConeErrorType(errorExpression.reason))
        return errorExpression
    }

    // ---- 辅助方法 ----

    /**
     * 有接收者的名称解析：在接收者类型的成员 scope 中查找。
     */
    private fun resolveWithReceiver(name: Name, receiver: CfirExpression): ConeCangjieType {
        val receiverType = receiver.coneTypeOrNull
            ?: return ConeErrorType("receiver has no type")

        val memberScope = getMemberScope(receiverType)
            ?: return ConeErrorType("no member scope for type: $receiverType")

        val candidates = mutableListOf<CfirCallableSymbol<*>>()
        memberScope.processPropertiesByName(name) { candidates.add(it) }
        memberScope.processFunctionsByName(name) { candidates.add(it) }

        return if (candidates.isEmpty()) {
            ConeErrorType(ConeUnresolvedNameError(name, receiverType = receiverType))
        } else {
            extractTypeFromCallableSymbol(candidates.first())
        }
    }

    /** 获取类型对应的成员 scope（含继承成员） */
    private fun getMemberScope(type: ConeCangjieType): CfirClassUseSiteMemberScope? {
        val classId = when (type) {
            is ConeClassLikeType -> type.classId
            is ConeStructType -> type.classId
            is ConeEnumType -> type.classId
            else -> return null
        }
        val classSymbol = components.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        return CfirClassUseSiteMemberScope(classSymbol, components.symbolProvider)
    }

    /** 从可调用符号中提取类型 */
    private fun extractTypeFromCallableSymbol(symbol: CfirCallableSymbol<*>): ConeCangjieType {
        if (!symbol.isBound) return ConeErrorType("unbound symbol")
        val typeRef = when (val decl = symbol.cfir) {
            is CfirFunction -> decl.returnTypeRef
            is CfirProperty -> decl.returnTypeRef
            is CfirVariable -> decl.returnTypeRef
            is CfirPatternVariable -> decl.returnTypeRef
            is CfirValueParameter -> decl.returnTypeRef
            else -> return ConeErrorType("unsupported callable declaration: ${decl::class.simpleName}")
        }
        return if (typeRef is CfirResolvedTypeRef) {
            typeRef.coneType
        } else {
            ConeErrorType("unresolved type for ${symbol::class.simpleName}")
        }
    }

    /** 从任意符号中提取类型 */
    private fun extractTypeFromSymbol(symbol: CfirSymbol<*>): ConeCangjieType {
        return when (symbol) {
            is CfirCallableSymbol<*> -> extractTypeFromCallableSymbol(symbol)
            is CfirClassSymbol -> {
                // Phase 2 不处理类型引用，CfirClass 没有 classId 属性
                ConeErrorType("class type reference")
            }
            else -> ConeErrorType("unsupported symbol type: ${symbol::class.simpleName}")
        }
    }

    /** 内建操作符回退：从 functionCall 提取接收者和参数类型，委托给 CfirBuiltinOperatorResolver */
    private fun tryBuiltinOperatorFallback(
        functionCall: CfirFunctionCall,
        reference: CfirNamedReference,
    ): ConeCangjieType? {
        val receiverType = functionCall.explicitReceiver?.coneTypeOrNull
        val argTypes = functionCall.arguments.mapNotNull { it.coneTypeOrNull }
        return CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
            reference.name, receiverType, argTypes
        )
    }

    /** 从函数符号中提取返回类型 */
    private fun extractReturnTypeFromSymbol(symbol: CfirSymbol<*>): ConeCangjieType {
        if (symbol is CfirFunctionSymbol && symbol.isBound) {
            val typeRef = symbol.cfir.returnTypeRef
            return if (typeRef is CfirResolvedTypeRef) typeRef.coneType
            else ConeErrorType("unresolved return type")
        }
        return extractTypeFromSymbol(symbol)
    }

    // ---- for-in 循环 ----

    override fun transformForInExpression(
        forInExpression: CfirForInExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 先递归解析迭代对象
        forInExpression.iterable.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)

        // 推断迭代变量类型
        val iterableType = forInExpression.iterable.coneTypeOrNull
        val iterVarType = inferIterableElementType(iterableType)

        // 将迭代变量类型写回（通过 returnTypeRef 替换）
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

        // 递归解析循环体
        // forInExpression.body.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)

        // for-in 返回 Unit
        forInExpression.replaceConeTypeOrNull(builtinTypes.unitType)
        return forInExpression
    }

    /** 从可迭代类型推断元素类型：Range<T> -> T，Iterable<T> -> T，否则返回 ConeErrorType */
    private fun inferIterableElementType(iterableType: ConeCangjieType?): ConeCangjieType {
        if (iterableType == null) return ConeErrorType("iterable has no type")

        // Range<T>：取第一个类型参数
        if (iterableType is ConeClassLikeType && iterableType.classId == StdlibClassIds.Range) {
            return iterableType.typeArguments.firstOrNull() ?: ConePrimitiveType.INT64
        }
        if (iterableType is ConeStructType && iterableType.classId == StdlibClassIds.Range) {
            return iterableType.typeArguments.firstOrNull() ?: ConePrimitiveType.INT64
        }

        // Iterable<T>：取第一个类型参数
        if (iterableType is ConeClassLikeType) {
            val typeArgs = iterableType.typeArguments
            if (typeArgs.isNotEmpty()) return typeArgs.first()
        }

        return ConeErrorType("cannot infer element type from: $iterableType")
    }

    // ---- loop / while 循环 ----

    override fun transformLoopExpression(
        loopExpression: CfirLoopExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 递归解析条件和循环体
        loopExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // loop/while 返回 Unit
        loopExpression.replaceConeTypeOrNull(builtinTypes.unitType)
        return loopExpression
    }

    // ---- throw 表达式 ----

    override fun transformThrowExpression(
        throwExpression: CfirThrowExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        throwExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // throw 返回 Nothing
        throwExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        return throwExpression
    }

    // ---- try/catch 表达式 ----

    override fun transformTryExpression(
        tryExpression: CfirTryExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        tryExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // 收集所有分支类型：try 块 + 每个 catch 块
        val branchTypes = mutableListOf<ConeCangjieType>()
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

    /** 公共超类型：不同类型使用 ConeUnionType 表示，后续由推断阶段细化 */
    private fun commonSupertype(types: List<ConeCangjieType>): ConeCangjieType {
        if (types.isEmpty()) return builtinTypes.unitType
        val first = types.first()
        if (types.all { it == first }) return first
        return ConeUnionType(types.toSet())
    }

    // ---- subscript 下标访问 ----

    override fun transformSubscriptExpression(
        subscriptExpression: CfirSubscriptExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        subscriptExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)
        val resultType = when (val receiverType = subscriptExpression.receiver.coneTypeOrNull) {
            is ConeTupleType -> {
                // Tuple 下标：取对应位置元素类型
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
                // 其他类型：查找 [] 操作符重载
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

    /** 从常量整数表达式提取下标值 */
    private fun extractConstantIntIndex(expr: CfirExpression?): Int? {
        if (expr !is CfirLiteralExpression) return null
        if (expr.kind != CfirLiteralKind.INT) return null
        return (expr.value as? Long)?.toInt() ?: (expr.value as? Int)
    }

    // ---- lambda 表达式 ----

    override fun transformLambdaExpression(
        lambdaExpression: CfirLambdaExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        val anonFunc = lambdaExpression.anonymousFunction

        // 递归解析匿名函数体
        anonFunc.body?.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)

        // 参数类型：从显式注解提取，无法推断时用 ConeErrorType
        val expectedType = (data as? CfirResolutionMode.WithExpectedType)?.expectedType as? ConeFuncType
        val paramTypes = anonFunc.valueParameters.mapIndexed { i, param ->
            val explicit = param.returnTypeRef
            if (explicit is CfirResolvedTypeRef) {
                explicit.coneType
            } else {
                expectedType?.parameterTypes?.getOrNull(i) ?: ConeErrorType("cannot infer lambda param type at $i")
            }
        }

        // 返回类型：从期望类型或函数体最后一个表达式推断
        val returnType = when {
            expectedType != null -> expectedType.returnType
            anonFunc.returnTypeRef is CfirResolvedTypeRef -> (anonFunc.returnTypeRef as CfirResolvedTypeRef).coneType
            else -> anonFunc.body?.coneTypeOrNull ?: ConeErrorType("cannot infer lambda return type")
        }

        lambdaExpression.replaceConeTypeOrNull(ConeFuncType(paramTypes, returnType))
        return lambdaExpression
    }

    // ---- range 表达式 ----

    override fun transformRangeExpression(
        rangeExpression: CfirRangeExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        rangeExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // 元素类型从 start/end 操作数推断，默认 Int64
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

    // ---- spawn 并发表达式 ----

    override fun transformSpawnExpression(
        spawnExpression: CfirSpawnExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        spawnExpression.transformChildren(transformer, CfirResolutionMode.ContextIndependent)

        // 任务返回类型：从 spawn 块的类型提取
        val taskReturnType = spawnExpression.body.coneTypeOrNull ?: builtinTypes.unitType
        spawnExpression.replaceConeTypeOrNull(
            ConeClassLikeType(ConeClassLookupTagImpl(StdlibClassIds.Future), listOf(taskReturnType))
        )
        return spawnExpression
    }
}