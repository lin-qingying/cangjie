package org.cangnova.cangjie.analysis.low.level.api.cfir.resolver

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 为只需要访问解析组件的场景创建占位 body resolve 组件集合。
 *
 * 返回的组件不会用于实际树变换，主要服务 IDE 查询路径中的候选收集和局部解析辅助逻辑。
 */
internal fun createStubBodyResolveComponents(cfirSession: CfirSession): CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents {
    val scopeSession = ScopeSession()

    // This transformer is not intended for actual transformations and created here only to simplify access to resolve components
    val stubBodyResolveTransformer = CfirBodyResolveTransformer(
        session = cfirSession,
        phase = CfirResolvePhase.BODY_RESOLVE,
        implicitTypeOnly = false,
        scopeSession = scopeSession,
    )

    return StubBodyResolveTransformerComponents(
        cfirSession,
        scopeSession,
        stubBodyResolveTransformer,
        stubBodyResolveTransformer.context,
    )
}

/**
 * 基于主干 body resolve transformer 组件基类的占位组件实现。
 *
 * 该类暴露 call resolver、tower resolver、call completer 等主干解析能力，同时强制展开 typealias，
 * 以匹配 low-level API 查询路径需要的类型解析行为。
 */
internal open class StubBodyResolveTransformerComponents(
    session: CfirSession,
    scopeSession: ScopeSession,
    transformer: CfirBodyResolveTransformer,
    context: BodyResolveContext,
) : CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents(
    session,
    scopeSession,
    transformer,
    context,
    expandTypeAliases = true,
)
