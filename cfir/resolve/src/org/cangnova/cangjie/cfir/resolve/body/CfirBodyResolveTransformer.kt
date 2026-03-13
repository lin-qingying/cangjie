package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitSimpleImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitStarImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirTypeParameterScopeImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType

/**
 * Body 解析 transformer — 具体 dispatcher。
 *
 * 管理声明级别的遍历和 scope 栈：
 * - [transformFile]：推入导入 scope
 * - [transformClass]：推入类成员 scope + 类型参数 scope
 * - [transformFunction]：推入值参数 scope → 解析函数体
 * - [transformProperty]：解析 initializer
 * - [transformVariable]：解析 initializer，将变量加入局部 scope
 *
 * 所有表达式节点委托给 [expressionsTransformer]。
 *
 * 参考 K2 FirBodyResolveTransformer / FirAbstractBodyResolveTransformerDispatcher。
 */
class CfirBodyResolveTransformer(
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirAbstractBodyResolveTransformerDispatcher(CfirResolvePhase.BODY_RESOLVE) {

    override val context: CfirBodyResolveContext = CfirBodyResolveContext()

    override val components: BodyResolveTransformerComponents =
        BodyResolveTransformerComponents(session, scopeSession, this, context)

    private val expressionsTransformer = CfirExpressionsResolveTransformer(this)

    // ---- 声明遍历 ----

    override fun transformFile(file: CfirFile, data: CfirResolutionMode): CfirFile {
        checkSessionConsistency(file)
        val savedContext = context.towerDataContext
        context.withFile(file) {
            // 推入导入 scope（包成员 + 简单导入 + 星号导入）
            val importScopes = createImportingScopes(file)
            context.addNonLocalScopes(importScopes)

            // 遍历所有声明
            file.declarations = file.declarations.map { decl ->
                decl.transform<CfirDeclaration, CfirResolutionMode>(this, CfirResolutionMode.ContextIndependent)
            }
        }
        context.withTowerDataContext(savedContext) {} // 恢复
        return file
    }

    override fun transformClass(klass: CfirClass, data: CfirResolutionMode): CfirElement {
        val savedContext = context.towerDataContext

        context.withContainer(klass) {
            // 推入类型参数 scope
            if (klass.typeParameters.isNotEmpty()) {
                context.addNonLocalScope(CfirTypeParameterScopeImpl(klass.typeParameters))
            }

            // 推入类成员 scope
            val classSymbol = klass.symbol as? CfirClassSymbol
            if (classSymbol != null) {
                context.addNonLocalScope(CfirClassDeclaredMemberScope(classSymbol))
            }

            // 遍历类成员
            klass.declarations = klass.declarations.map { decl ->
                decl.transform<CfirDeclaration, CfirResolutionMode>(this, CfirResolutionMode.ContextIndependent)
            }
        }

        context.withTowerDataContext(savedContext) {} // 恢复
        bumpPhase(klass)
        return klass
    }

    override fun transformFunction(function: CfirFunction, data: CfirResolutionMode): CfirElement {
        val savedContext = context.towerDataContext

        context.withContainer(function) {
            // 推入类型参数 scope
            if (function.typeParameters.isNotEmpty()) {
                context.addNonLocalScope(CfirTypeParameterScopeImpl(function.typeParameters))
            }

            // 推入值参数局部 scope
            val paramScope = CfirLocalScopeImpl()
            for (param in function.valueParameters) {
                val paramSymbol = param.symbol as? CfirVariableSymbol ?: continue
                paramScope.addVariable(param.name, paramSymbol)
            }
            context.addLocalScope(paramScope)

            // 解析函数体
            val body = function.body
            if (body != null) {
                function.body = body.transform<CfirBlock, CfirResolutionMode>(
                    this, CfirResolutionMode.ContextIndependent
                )
            }
        }

        context.withTowerDataContext(savedContext) {} // 恢复
        bumpPhase(function)
        return function
    }

    override fun transformProperty(property: CfirProperty, data: CfirResolutionMode): CfirElement {
        val savedContext = context.towerDataContext

        context.withContainer(property) {
            // 解析 initializer
            val initializer = property.initializer
            if (initializer != null) {
                property.initializer = initializer.transform<CfirExpression, CfirResolutionMode>(
                    this, CfirResolutionMode.ContextIndependent
                )
            }
        }

        context.withTowerDataContext(savedContext) {} // 恢复
        bumpPhase(property)
        return property
    }

    override fun transformVariable(variable: CfirVariable, data: CfirResolutionMode): CfirElement {
        // 解析 initializer
        val initializer = variable.initializer
        if (initializer != null) {
            variable.initializer = initializer.transform<CfirExpression, CfirResolutionMode>(
                this, CfirResolutionMode.ContextIndependent
            )
        }

        // 将变量加入局部 scope
        val varSymbol = variable.symbol as? CfirVariableSymbol
        if (varSymbol != null) {
            context.storeVariable(variable.name, varSymbol)
        }

        bumpPhase(variable)
        return variable
    }

    // ---- 表达式委托 ----

    override fun transformExpression(expression: CfirExpression, data: CfirResolutionMode): CfirElement {
        return expressionsTransformer.transformExpression(expression, data)
    }

    override fun transformBlock(block: CfirBlock, data: CfirResolutionMode): CfirElement {
        // 块内创建新的局部 scope
        val savedContext = context.towerDataContext
        val blockScope = CfirLocalScopeImpl()
        context.addLocalScope(blockScope)

        val result = expressionsTransformer.transformBlock(block, data)

        context.withTowerDataContext(savedContext) {} // 恢复
        return result
    }

    // 委托所有具体表达式类型到 expressionsTransformer
    override fun transformLiteralExpression(literalExpression: org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression, data: CfirResolutionMode) =
        expressionsTransformer.transformLiteralExpression(literalExpression, data)

    override fun transformPropertyAccess(propertyAccess: org.cangnova.cangjie.cfir.expressions.CfirPropertyAccess, data: CfirResolutionMode) =
        expressionsTransformer.transformPropertyAccess(propertyAccess, data)

    override fun transformQualifiedAccess(qualifiedAccess: org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccess, data: CfirResolutionMode) =
        expressionsTransformer.transformQualifiedAccess(qualifiedAccess, data)

    override fun transformFunctionCall(functionCall: org.cangnova.cangjie.cfir.expressions.CfirFunctionCall, data: CfirResolutionMode) =
        expressionsTransformer.transformFunctionCall(functionCall, data)

    override fun transformIfExpression(ifExpression: org.cangnova.cangjie.cfir.expressions.CfirIfExpression, data: CfirResolutionMode) =
        expressionsTransformer.transformIfExpression(ifExpression, data)

    override fun transformReturnExpression(returnExpression: org.cangnova.cangjie.cfir.expressions.CfirReturnExpression, data: CfirResolutionMode) =
        expressionsTransformer.transformReturnExpression(returnExpression, data)

    override fun transformAssignment(assignment: org.cangnova.cangjie.cfir.expressions.CfirAssignment, data: CfirResolutionMode) =
        expressionsTransformer.transformAssignment(assignment, data)

    override fun transformTupleLiteral(tupleLiteral: org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral, data: CfirResolutionMode) =
        expressionsTransformer.transformTupleLiteral(tupleLiteral, data)

    override fun transformArrayLiteral(arrayLiteral: org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral, data: CfirResolutionMode) =
        expressionsTransformer.transformArrayLiteral(arrayLiteral, data)

    override fun transformStringInterpolation(stringInterpolation: org.cangnova.cangjie.cfir.expressions.CfirStringInterpolation, data: CfirResolutionMode) =
        expressionsTransformer.transformStringInterpolation(stringInterpolation, data)

    override fun transformErrorExpression(errorExpression: org.cangnova.cangjie.cfir.expressions.CfirErrorExpression, data: CfirResolutionMode) =
        expressionsTransformer.transformErrorExpression(errorExpression, data)

    // ---- 默认声明处理 ----

    override fun transformDeclaration(declaration: CfirDeclaration, data: CfirResolutionMode): CfirDeclaration {
        bumpPhase(declaration)
        return declaration
    }

    // ---- 辅助方法 ----

    /** 创建文件的导入 scope 列表 */
    private fun createImportingScopes(file: CfirFile): List<org.cangnova.cangjie.cfir.scopes.CfirScope> {
        val symbolProvider = session.symbolProvider
        val imports = file.imports
        return buildList {
            // 包成员 scope
            add(CfirPackageMemberScope(file.packageDirective.packageFqName, symbolProvider))
            // 精确导入 scope
            add(CfirExplicitSimpleImportingScope(imports, symbolProvider))
            // 星号导入 scope
            add(CfirExplicitStarImportingScope(imports, symbolProvider))
        }
    }

    /** 推进声明的 resolvePhase */
    private fun bumpPhase(declaration: CfirDeclaration) {
        if (declaration.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES &&
            declaration.resolvePhase < CfirResolvePhase.BODY_RESOLVE
        ) {
            declaration.resolvePhase = CfirResolvePhase.BODY_RESOLVE
        }
    }

    // ---- 默认 transformElement ----

    override fun <E : CfirElement> transformElement(element: E, data: CfirResolutionMode): E {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E)
    }
}
