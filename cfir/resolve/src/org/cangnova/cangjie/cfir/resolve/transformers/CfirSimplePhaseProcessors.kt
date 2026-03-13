package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * IMPLICIT_TYPES 阶段 processor。
 *
 * Phase 2 基础实现：
 * - 有显式返回类型的函数/属性 → 直接使用已解析的 returnTypeRef.coneType
 * - 无显式返回类型 → 暂标记为已完成（Phase 3 实现完整推断）
 */
internal class CfirImplicitTypesResolveProcessor(
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.IMPLICIT_TYPES,
) {
    override val transformer: CfirImplicitTypesResolveTransformer =
        CfirImplicitTypesResolveTransformer(session)
}

/**
 * BODY_RESOLVE 阶段 processor。
 *
 * 使用 [CfirBodyResolveTransformer] 进行表达式级别的类型合成。
 * 覆写 [processFile] 以传递正确的 [CfirResolutionMode] 数据。
 */
internal class CfirBodyResolveProcessor(
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.BODY_RESOLVE,
) {
    private val bodyResolveTransformer = CfirBodyResolveTransformer(session, scopeSession)

    @Suppress("UNCHECKED_CAST")
    override val transformer get() = bodyResolveTransformer as org.cangnova.cangjie.cfir.visitors.CfirTransformer<Nothing?>

    override fun processFile(file: CfirFile) {
        bodyResolveTransformer.transformFile(file, CfirResolutionMode.ContextIndependent)
    }
}

/**
 * IMPLICIT_TYPES 阶段 transformer。
 *
 * Phase 2 基础实现：推进 resolvePhase 到 IMPLICIT_TYPES。
 * 后续 Phase 3 将添加完整的隐式类型推断逻辑。
 */
internal class CfirImplicitTypesResolveTransformer(
    override val session: CfirSession,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.IMPLICIT_TYPES) {
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase >= CfirResolvePhase.EXTENSIONS && declaration.resolvePhase < CfirResolvePhase.IMPLICIT_TYPES) {
            declaration.resolvePhase = CfirResolvePhase.IMPLICIT_TYPES
        }
        return declaration
    }
}
