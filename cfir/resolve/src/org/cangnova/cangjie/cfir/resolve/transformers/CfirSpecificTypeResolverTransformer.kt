/*
 * Copyright 2010-2026. cangjie.
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.CfirExplicitTypeRefResolver
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/**
 * 类型引用解析委托器（TYPES 阶段）。
 *
 * 桥接树遍历与类型解析：
 * - [CfirTypeResolveTransformer] 负责树遍历和 scope 管理
 * - 本类负责将单个类型引用委托给 [CfirExplicitTypeRefResolver] 进行解析
 * - [CfirExplicitTypeRefResolver] 负责具体的类型解析逻辑
 *
 * data 参数为当前作用域内可见的类型参数映射 `Map<String, CfirTypeParameter>`。
 *
 * 对齐 K2: `FirSpecificTypeResolverTransformer`
 */
class CfirSpecificTypeResolverTransformer(
    override val session: CfirSession,
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirAbstractTreeTransformer<Map<String, CfirTypeParameter>>(CfirResolvePhase.TYPES) {

    private val explicitTypeRefResolver = CfirExplicitTypeRefResolver(session, diagnosticReporter)

    override fun transformTypeRef(
        typeRef: CfirTypeRef,
        data: Map<String, CfirTypeParameter>,
    ): CfirTypeRef {
        return explicitTypeRefResolver.resolveExplicitTypeRef(typeRef, data)
    }

    override fun transformResolvedTypeRef(
        resolvedTypeRef: CfirResolvedTypeRef,
        data: Map<String, CfirTypeParameter>,
    ): CfirTypeRef {
        // 已解析 → 直接返回
        return resolvedTypeRef
    }

    override fun transformImplicitTypeRef(
        implicitTypeRef: CfirImplicitTypeRef,
        data: Map<String, CfirTypeParameter>,
    ): CfirTypeRef {
        // 隐式类型 → 跳过，留给 IMPLICIT_TYPES 阶段处理
        return implicitTypeRef
    }
}
