package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractPhaseTransformer
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider

/**
 * Body 解析 transformer 抽象基类。
 *
 * 定义 body resolve 阶段的三个核心抽象属性：
 * - [context]：Body 解析上下文（scope 塔、文件、容器栈）
 * - [components]：共享组件容器（session、call resolver、tower resolver 等）
 *
 * session 统一从 components 获取，避免各子组件直接互相持有引用。
 *
 * 参考 K2 FirAbstractBodyResolveTransformer。
 */
abstract class CfirAbstractBodyResolveTransformer(
    phase: CfirResolvePhase,
) : CfirAbstractPhaseTransformer<CfirResolutionMode>(phase) {

    abstract val context: CfirBodyResolveContext

    abstract val components: BodyResolveTransformerComponents

    final override val session: CfirSession get() = components.session

    /**
     * 共享组件容器，所有 body resolve 子组件通过此容器协作。
     *
     * 持有 session、scopeSession、context 引用，
     * 以及懒初始化的 callResolver、towerResolver 等。
     *
     * 参考 K2 FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents。
     */
    open class BodyResolveTransformerComponents(
        override val session: CfirSession,
        val scopeSession: CfirScopeSession,
        val transformer: CfirAbstractBodyResolveTransformerDispatcher,
        val context: CfirBodyResolveContext,
    ) : CfirSessionHolder {

        /** scope 塔上下文 — 委托到 context */
        val towerDataContext get() = context.towerDataContext

        /** 符号提供器 — 委托到 session */
        val symbolProvider get() = session.symbolProvider

        /** 候选验证管线执行器 — 即时初始化（无状态，轻量） */
        val resolutionStageRunner: CfirResolutionStageRunner = CfirResolutionStageRunner()

        /** Tower 解析器 — 懒初始化 */
        val towerResolver: CfirTowerResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirTowerResolver(this, resolutionStageRunner)
        }

        /** 调用解析器 — 懒初始化 */
        val callResolver: CfirCallResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirCallResolver(this)
        }
    }
}

/**
 * Body 解析 dispatcher 抽象基类。
 *
 * 作为具体 dispatcher（如 [CfirBodyResolveTransformer]）的基类，
 * 持有 context 和 components 的所有权。
 *
 * 参考 K2 FirAbstractBodyResolveTransformerDispatcher。
 */
abstract class CfirAbstractBodyResolveTransformerDispatcher(
    phase: CfirResolvePhase,
) : CfirAbstractBodyResolveTransformer(phase) {

    abstract override val context: CfirBodyResolveContext

    abstract override val components: BodyResolveTransformerComponents
}
