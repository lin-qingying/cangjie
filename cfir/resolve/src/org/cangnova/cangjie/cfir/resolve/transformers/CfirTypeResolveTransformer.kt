/*
 * Copyright 2010-2026. cangjie.
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/**
 * TYPES 阶段处理器。
 *
 * 负责将声明头中的所有显式类型引用解析为 [CfirResolvedTypeRef]。
 * 不处理函数体（留给 BODY_RESOLVE）和隐式类型（留给 IMPLICIT_TYPES）。
 *
 * 对齐 K2: `FirTypeResolveProcessor`
 */
class CfirTypeResolveProcessor(
    session: CfirSession,
    scopeSession: CfirScopeSession,
    diagnosticReporter: CfirDiagnosticReporter,
) : CfirTransformerBasedResolveProcessor(session, scopeSession, CfirResolvePhase.TYPES) {
    override val transformer = CfirTypeResolveTransformer(session, diagnosticReporter)
}

/**
 * TYPES 阶段转换器。
 *
 * 遍历 CFIR 树，将声明头中的显式类型引用（`CfirUserTypeRef`、`CfirFunctionTypeRef`、
 * `CfirTupleTypeRef`、`CfirVArrayTypeRef`）解析为 `CfirResolvedTypeRef`。
 *
 * 核心职责：
 * - 函数返回类型（`CfirFunction.returnTypeRef`）
 * - 属性类型（`CfirProperty.returnTypeRef`）
 * - 变量类型（`CfirVariable.returnTypeRef`）
 * - 值参数类型（`CfirValueParameter.returnTypeRef`）
 * - 构造函数参数和返回类型
 * - 类型参数边界（`CfirTypeParameter.bounds`）
 * - 类型别名展开类型（`CfirTypeAlias.expandedTypeRef`）
 * - extend 块的扩展类型（`CfirExtend.extendedTypeRef`）
 *
 * 设计决策：
 * - 类型参数 scope 用 `Map<String, CfirTypeParameter>` 简化（K2 用 PersistentList<FirScope>）
 * - `transformBlock` 直接返回，TYPES 阶段只处理声明头
 * - 三层委托：本类（树遍历 + scope 管理）→ [CfirSpecificTypeResolverTransformer]（类型引用解析委托）
 *   → [CfirExplicitTypeRefResolver][org.cangnova.cangjie.cfir.resolve.CfirExplicitTypeRefResolver]（具体解析逻辑）
 *
 * 对齐 K2: `FirTypeResolveTransformer`
 */
class CfirTypeResolveTransformer(
    override val session: CfirSession,
    diagnosticReporter: CfirDiagnosticReporter,
) : CfirAbstractTreeTransformer<Any?>(CfirResolvePhase.TYPES) {

    private val typeResolverTransformer = CfirSpecificTypeResolverTransformer(session, diagnosticReporter)

    /**
     * 当前作用域内可见的类型参数（由外向内累积）。
     *
     * 遍历嵌套声明时通过 [withTypeParameters] 临时加入/移除类型参数。
     * Key 为类型参数名称，Value 为对应的 [CfirTypeParameter] 声明。
     */
    private val typeParametersInScope = mutableMapOf<String, CfirTypeParameter>()

    // ---- 声明遍历 ----

    override fun transformFile(file: CfirFile, data: Any?): CfirFile {
        checkSessionConsistency(file)
        file.transformDeclarations(this, data)
        return file
    }

    override fun transformClass(klass: CfirClass, data: Any?): CfirDeclaration {
        return withTypeParameters(klass.typeParameters) {
            // 先解析类型参数自身的 bounds
            klass.transformTypeParameters(this, data)
            // 再遍历成员声明
            klass.transformDeclarations(this, data)
            bumpPhase(klass)
            klass
        }
    }

    override fun transformExtend(extend: CfirExtend, data: Any?): CfirDeclaration {
        return withTypeParameters(extend.typeParameters) {
            extend.transformTypeParameters(this, data)
            extend.transformExtendedTypeRef(this, data)
            extend.transformSuperTypeRefs(this, data)
            extend.transformDeclarations(this, data)
            bumpPhase(extend)
            extend
        }
    }

    override fun transformFunction(function: CfirFunction, data: Any?): CfirDeclaration {
        return withTypeParameters(function.typeParameters) {
            function.transformTypeParameters(this, data)
            function.transformReturnTypeRef(this, data)
            function.transformValueParameters(this, data)
            // 不遍历 body — TYPES 阶段不解析函数体
            bumpPhase(function)
            function
        }
    }

    override fun transformConstructor(constructor: CfirConstructor, data: Any?): CfirDeclaration {
        return withTypeParameters(constructor.typeParameters) {
            constructor.transformTypeParameters(this, data)
            constructor.transformReturnTypeRef(this, data)
            constructor.transformValueParameters(this, data)
            bumpPhase(constructor)
            constructor
        }
    }

    override fun transformProperty(property: CfirProperty, data: Any?): CfirDeclaration {
        return withTypeParameters(property.typeParameters) {
            property.transformTypeParameters(this, data)
            property.transformReturnTypeRef(this, data)
            bumpPhase(property)
            property
        }
    }

    override fun transformVariable(variable: CfirVariable, data: Any?): CfirDeclaration {
        variable.transformReturnTypeRef(this, data)
        bumpPhase(variable)
        return variable
    }

    override fun transformValueParameter(valueParameter: CfirValueParameter, data: Any?): CfirDeclaration {
        valueParameter.transformReturnTypeRef(this, data)
        return valueParameter
    }

    override fun transformTypeParameter(typeParameter: CfirTypeParameter, data: Any?): CfirDeclaration {
        typeParameter.transformBounds(this, data)
        return typeParameter
    }

    override fun transformTypeAlias(typeAlias: CfirTypeAlias, data: Any?): CfirDeclaration {
        return withTypeParameters(typeAlias.typeParameters) {
            typeAlias.transformTypeParameters(this, data)
            typeAlias.transformExpandedTypeRef(this, data)
            bumpPhase(typeAlias)
            typeAlias
        }
    }

    // ---- 类型解析 ----

    override fun transformTypeRef(typeRef: CfirTypeRef, data: Any?): CfirTypeRef {
        // 委托到 CfirSpecificTypeResolverTransformer，传入当前 scope 中的类型参数
        return typeResolverTransformer.transformTypeRef(typeRef, typeParametersInScope)
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: Any?): CfirTypeRef {
        // 已解析 → 直接返回
        return resolvedTypeRef
    }

    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: Any?): CfirTypeRef {
        // 隐式类型 → 留给 IMPLICIT_TYPES 阶段
        return implicitTypeRef
    }

    // ---- 跳过 ----

    override fun transformBlock(block: CfirBlock, data: Any?): CfirExpression {
        // TYPES 阶段不解析函数体
        return block
    }

    // ---- 辅助 ----

    /**
     * 临时将 [params] 加入类型参数 scope，执行 [action]，然后恢复。
     *
     * 支持嵌套调用：外层类型参数在内层仍然可见，
     * 但内层同名类型参数会遮蔽外层。
     */
    private inline fun <R> withTypeParameters(
        params: List<CfirTypeParameter>,
        action: () -> R,
    ): R {
        if (params.isEmpty()) return action()

        val savedEntries = mutableMapOf<String, CfirTypeParameter?>()
        for (param in params) {
            val name = param.name.asString()
            savedEntries[name] = typeParametersInScope.put(name, param)
        }
        return try {
            action()
        } finally {
            for ((name, previous) in savedEntries) {
                if (previous != null) {
                    typeParametersInScope[name] = previous
                } else {
                    typeParametersInScope.remove(name)
                }
            }
        }
    }

    /**
     * 推进声明的 resolvePhase（SUPER_TYPES → TYPES）。
     */
    private fun bumpPhase(declaration: CfirDeclaration) {
        declaration.replaceResolvePhase(CfirResolvePhase.TYPES)
    }
}
