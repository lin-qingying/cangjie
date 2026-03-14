package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.*
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef

/**
 * 声明处理子 transformer。
 *
 * 负责声明级别的遍历和 scope 栈管理：
 * - [transformFile]：推入导入 scope
 * - [transformClass]：推入类成员 scope + 类型参数 scope
 * - [transformFunction]：推入值参数 scope → 解析函数体
 * - [transformProperty]：解析 initializer
 * - [transformVariable]：解析 initializer，将变量加入局部 scope
 * - [transformBlock]：创建局部 scope + 委托到 expressionsTransformer
 *
 * 参考 K2 FirDeclarationsResolveTransformer。
 */
class CfirDeclarationsResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
) : CfirPartialBodyResolveTransformer(transformer) {

    // ---- 文件 ----

    override fun transformFile(file: CfirFile, data: CfirResolutionMode): CfirFile {
        val savedContext = context.towerDataContext
        context.withFile(file) {
            // 推入导入 scope（包成员 + 简单导入 + 星号导入）
            val importScopes = createImportingScopes(file)
            context.addNonLocalScopes(importScopes)

            // 遍历所有声明
            file.declarations = file.declarations.map { decl ->
                decl.transform<CfirDeclaration, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
            }
        }
        context.withTowerDataContext(savedContext) {} // 恢复
        return file
    }

    // ---- 类 ----

    override fun transformClass(klass: CfirClass, data: CfirResolutionMode): CfirDeclaration {
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

                // 推入 extend 成员 scope（Phase 4：extend 成员查找完善）
                val extendProvider = components.extendProvider
                if (extendProvider != null) {
                    val classId = resolveClassId(klass)
                    if (classId != null) {
                        context.addNonLocalScope(CfirExtendMemberScope(classId, extendProvider))
                    }
                }
            }

            // 遍历类成员
            klass.declarations = klass.declarations.map { decl ->
                decl.transform<CfirDeclaration, CfirResolutionMode>(transformer, CfirResolutionMode.ContextIndependent)
            }
        }

        context.withTowerDataContext(savedContext) {} // 恢复
        bumpPhase(klass)
        return klass
    }

    // ---- 函数 ----

    override fun transformFunction(function: CfirFunction, data: CfirResolutionMode): CfirDeclaration {
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
                    transformer, CfirResolutionMode.ContextIndependent
                )
            }

            // 隐式返回类型推断：从函数体最后一个表达式推断
            if (function.returnTypeRef is CfirImplicitTypeRef) {
                val bodyType = function.body?.coneTypeOrNull ?: session.builtinTypes.unitType
                function.replaceReturnTypeRef(buildResolvedTypeRef { coneType = bodyType })
            }
        }

        context.withTowerDataContext(savedContext) {} // 恢复
        bumpPhase(function)
        return function
    }

    // ---- 属性 ----

    override fun transformProperty(property: CfirProperty, data: CfirResolutionMode): CfirDeclaration {
        val savedContext = context.towerDataContext

        context.withContainer(property) {
            // 解析 initializer
            val initializer = property.initializer
            if (initializer != null) {
                property.initializer = initializer.transform<CfirExpression, CfirResolutionMode>(
                    transformer, CfirResolutionMode.ContextIndependent
                )
            }

            // 隐式类型推断：从 initializer 推断
            if (property.returnTypeRef is CfirImplicitTypeRef) {
                val initType = property.initializer?.coneTypeOrNull
                if (initType != null) {
                    property.replaceReturnTypeRef(buildResolvedTypeRef { coneType = initType })
                }
            }
        }

        context.withTowerDataContext(savedContext) {} // 恢复
        bumpPhase(property)
        return property
    }

    // ---- 变量 ----

    override fun transformVariable(variable: CfirVariable, data: CfirResolutionMode): CfirDeclaration {
        // 解析 initializer
        val initializer = variable.initializer
        if (initializer != null) {
            variable.initializer = initializer.transform<CfirExpression, CfirResolutionMode>(
                transformer, CfirResolutionMode.ContextIndependent
            )
        }

        // 隐式类型推断：从 initializer 推断
        if (variable.returnTypeRef is CfirImplicitTypeRef) {
            val initType = variable.initializer?.coneTypeOrNull
            if (initType != null) {
                variable.replaceReturnTypeRef(buildResolvedTypeRef { coneType = initType })
            }
        }

        // 将变量加入局部 scope
        val varSymbol = variable.symbol as? CfirVariableSymbol
        if (varSymbol != null) {
            context.storeVariable(variable.name, varSymbol)
        }

        bumpPhase(variable)
        return variable
    }

    // ---- 默认声明处理 ----

    override fun transformDeclaration(declaration: CfirDeclaration, data: CfirResolutionMode): CfirDeclaration {
        bumpPhase(declaration)
        return declaration
    }

    // ---- 块（scope 管理 + 委托到 expressionsTransformer） ----

    override fun transformBlock(block: CfirBlock, data: CfirResolutionMode): CfirExpression {
        // 块内创建新的局部 scope
        val savedContext = context.towerDataContext
        val blockScope = CfirLocalScopeImpl()
        context.addLocalScope(blockScope)

        val result = transformer.expressionsTransformer.transformBlock(block, data)

        context.withTowerDataContext(savedContext) {} // 恢复
        return result
    }

    // ---- 辅助方法 ----

    /** 创建文件的导入 scope 列表 */
    private fun createImportingScopes(file: CfirFile): List<CfirScope> {
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

    /** 从类声明中解析 ClassId（用于 extend scope 查找） */
    private fun resolveClassId(klass: CfirClass): org.cangnova.cangjie.name.ClassId? {
        val packageFqName = try {
            context.file.packageDirective.packageFqName
        } catch (_: UninitializedPropertyAccessException) {
            org.cangnova.cangjie.name.FqName.ROOT
        }
        return org.cangnova.cangjie.name.ClassId(packageFqName, klass.name)
    }
}
