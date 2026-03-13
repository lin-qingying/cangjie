package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedNamedReferenceImpl
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*

/**
 * 表达式类型合成 transformer。
 *
 * 负责为每种表达式节点合成（synthesize）类型：
 * - 字面量 → 直接映射 ConeCangjieType
 * - 变量引用 → scope 塔查找 → 绑定符号 → 提取类型
 * - 属性访问 → 接收者类型 scope 查找
 * - 函数调用 → call resolver → 返回类型
 * - 复合表达式 → 递归合成 + 组合规则
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
        val type = synthesizeLiteralType(literalExpression.kind)
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
        if (receiver != null) {
            receiver.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
        }

        val reference = propertyAccess.calleeReference
        if (reference is CfirResolvedNamedReference) {
            // 已解析引用 — 直接从符号提取类型
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
                propertyAccess.replaceConeTypeOrNull(ConeErrorType("unresolved reference: $name"))
            } else {
                val symbol = candidates.first()
                // 绑定解析后的引用
                propertyAccess.calleeReference = CfirResolvedNamedReferenceImpl(name, symbol)
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
        if (receiver != null) {
            receiver.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
        }

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
                qualifiedAccess.replaceConeTypeOrNull(ConeErrorType("unresolved reference: $name"))
            } else {
                val symbol = candidates.first()
                qualifiedAccess.calleeReference = CfirResolvedNamedReferenceImpl(name, symbol)
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
        // 先递归解析显式接收者
        val receiver = functionCall.explicitReceiver
        if (receiver != null) {
            receiver.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
        }

        // 递归解析所有参数
        functionCall.arguments = functionCall.arguments.map { arg ->
            arg.transform<CfirExpression, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
        }

        val reference = functionCall.calleeReference
        if (reference is CfirResolvedNamedReference) {
            functionCall.replaceConeTypeOrNull(extractReturnTypeFromSymbol(reference.resolvedSymbol))
            return functionCall
        }

        if (reference !is CfirNamedReference) {
            functionCall.replaceConeTypeOrNull(ConeErrorType("non-name callee reference"))
            return functionCall
        }

        // 通过 callResolver 解析调用
        val result = callResolver.resolveCall(reference.name, functionCall.arguments)

        when (result) {
            is CfirCallResolutionResult.Success -> {
                functionCall.calleeReference = CfirResolvedNamedReferenceImpl(reference.name, result.symbol)
                functionCall.replaceConeTypeOrNull(result.returnType)
            }
            is CfirCallResolutionResult.Ambiguity -> {
                functionCall.replaceConeTypeOrNull(ConeErrorType("ambiguous call: ${reference.name}"))
            }
            is CfirCallResolutionResult.NoCandidate -> {
                functionCall.replaceConeTypeOrNull(ConeErrorType("unresolved call: ${reference.name}"))
            }
        }
        return functionCall
    }

    // ---- 块表达式 ----

    override fun transformBlock(block: CfirBlock, data: CfirResolutionMode): CfirExpression {
        // 逐语句解析（transformChildren 由 dispatcher 处理 scope）
        block.statements = block.statements.map { stmt ->
            stmt.transform<CfirElement, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
        }

        // 块的类型 = 最后一个表达式的类型，或 Unit
        val lastExpr = block.statements.lastOrNull()
        val blockType = if (lastExpr is CfirExpression) {
            lastExpr.coneTypeOrNull ?: builtinTypes.unitType
        } else {
            builtinTypes.unitType
        }
        block.replaceConeTypeOrNull(blockType)
        return block
    }

    // ---- if 表达式 ----

    override fun transformIfExpression(
        ifExpression: CfirIfExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 解析 condition（期望 Bool，但本阶段不强制检查）
        ifExpression.condition = ifExpression.condition
            .transform(transformer, CfirResolutionMode.WithExpectedType(builtinTypes.boolType))

        // 解析 then 分支
        ifExpression.thenBranch = ifExpression.thenBranch
            .transform(transformer, CfirResolutionMode.ContextIndependent)

        // 解析 else 分支
        val elseBranch = ifExpression.elseBranch
        if (elseBranch != null) {
            ifExpression.elseBranch = elseBranch
                .transform(transformer, CfirResolutionMode.ContextIndependent)
        }

        // if 表达式的类型 = then 和 else 分支的公共超类型
        val thenType = ifExpression.thenBranch.coneTypeOrNull
        val elseType = ifExpression.elseBranch?.let { (it as? CfirExpression)?.coneTypeOrNull }

        val resultType = when {
            thenType == null -> elseType ?: builtinTypes.unitType
            elseType == null -> builtinTypes.unitType // 无 else → Unit（语句用法）
            thenType == elseType -> thenType
            // Phase 2 简化：不同类型用 ConeUnionType 表示，后续由推断阶段细化
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
        val result = returnExpression.result
        if (result != null) {
            returnExpression.result = result
                .transform(transformer, CfirResolutionMode.ContextIndependent)
        }
        // return 表达式的类型 = Nothing
        returnExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        return returnExpression
    }

    // ---- 赋值表达式 ----

    override fun transformAssignment(
        assignment: CfirAssignment,
        data: CfirResolutionMode,
    ): CfirExpression {
        // 解析左值
        assignment.lValue = assignment.lValue
            .transform(transformer, CfirResolutionMode.ContextIndependent)
        // 解析右值
        assignment.rValue = assignment.rValue
            .transform(transformer, CfirResolutionMode.ContextIndependent)
        // 赋值类型 = Unit
        assignment.replaceConeTypeOrNull(builtinTypes.unitType)
        return assignment
    }

    // ---- 元组字面量 ----

    override fun transformTupleLiteral(
        tupleLiteral: CfirTupleLiteral,
        data: CfirResolutionMode,
    ): CfirExpression {
        tupleLiteral.elements = tupleLiteral.elements.map { elem ->
            elem.transform<CfirExpression, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
        }
        val elementTypes = tupleLiteral.elements.map { it.coneTypeOrNull ?: ConeErrorType("unresolved element") }
        tupleLiteral.replaceConeTypeOrNull(ConeTupleType(elementTypes))
        return tupleLiteral
    }

    // ---- 数组字面量 ----

    override fun transformArrayLiteral(
        arrayLiteral: CfirArrayLiteral,
        data: CfirResolutionMode,
    ): CfirExpression {
        arrayLiteral.elements = arrayLiteral.elements.map { elem ->
            elem.transform<CfirExpression, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
        }
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
        stringInterpolation.parts = stringInterpolation.parts.map { part ->
            part.transform<CfirExpression, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
        }
        stringInterpolation.replaceConeTypeOrNull(
            ConeClassLikeType(ConeClassLookupTagImpl(StdlibClassIds.String))
        )
        return stringInterpolation
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
    private fun resolveWithReceiver(name: org.cangnova.cangjie.name.Name, receiver: CfirExpression): ConeCangjieType {
        val receiverType = receiver.coneTypeOrNull
            ?: return ConeErrorType("receiver has no type")

        val memberScope = getMemberScope(receiverType)
            ?: return ConeErrorType("no member scope for type: $receiverType")

        val candidates = mutableListOf<CfirCallableSymbol<*>>()
        memberScope.processPropertiesByName(name) { candidates.add(it) }
        memberScope.processFunctionsByName(name) { candidates.add(it) }

        return if (candidates.isEmpty()) {
            ConeErrorType("unresolved member: $name on $receiverType")
        } else {
            extractTypeFromCallableSymbol(candidates.first())
        }
    }

    /** 获取类型对应的成员 scope */
    private fun getMemberScope(type: ConeCangjieType): CfirClassDeclaredMemberScope? {
        val classId = when (type) {
            is ConeClassLikeType -> type.classId
            is ConeStructType -> type.classId
            is ConeEnumType -> type.classId
            else -> return null
        }
        val classSymbol = components.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        return CfirClassDeclaredMemberScope(classSymbol)
    }

    /** 从可调用符号中提取类型 */
    private fun extractTypeFromCallableSymbol(symbol: CfirCallableSymbol<*>): ConeCangjieType {
        if (!symbol.isBound) return ConeErrorType("unbound symbol")
        val decl = symbol.cfir
        val typeRef = when (decl) {
            is CfirFunction -> decl.returnTypeRef
            is CfirProperty -> decl.returnTypeRef
            is CfirVariable -> decl.returnTypeRef
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

    /** 从函数符号中提取返回类型 */
    private fun extractReturnTypeFromSymbol(symbol: CfirSymbol<*>): ConeCangjieType {
        if (symbol is CfirFunctionSymbol && symbol.isBound) {
            val typeRef = symbol.cfir.returnTypeRef
            return if (typeRef is CfirResolvedTypeRef) typeRef.coneType
            else ConeErrorType("unresolved return type")
        }
        return extractTypeFromSymbol(symbol)
    }
}
