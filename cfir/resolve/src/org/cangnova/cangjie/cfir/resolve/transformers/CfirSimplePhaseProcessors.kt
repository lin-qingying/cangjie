package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirImplicitAwareBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirImplicitBodyResolveComputationSession
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.body.ReturnTypeCalculatorWithJump
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * IMPLICIT_TYPES 阶段处理器。
 * 使用 [CfirImplicitAwareBodyResolveTransformer] 推断隐式返回类型和隐式声明类型，
 * 并通过状态机避免递归依赖时重复计算。
 * 参考 K2 `FirImplicitTypeBodyResolveProcessor`。
 */
internal class CfirImplicitTypesResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.IMPLICIT_TYPES,
) {
    /** 隐式类型计算会话，缓存 callable 隐式返回类型状态。 */
    private val computationSession = CfirImplicitBodyResolveComputationSession()
    /** 支持 designated jump 的返回类型计算器。 */
    private val returnTypeCalculator = ReturnTypeCalculatorWithJump(session, scopeSession, computationSession)

    /** IMPLICIT_TYPES 阶段使用的 body resolve transformer。 */
    private val implicitTypesTransformer = CfirImplicitAwareBodyResolveTransformer(
        session = session,
        scopeSession = scopeSession,
        implicitBodyResolveComputationSession = computationSession,
        phase = CfirResolvePhase.IMPLICIT_TYPES,
        implicitTypeOnly = true,
        returnTypeCalculator = returnTypeCalculator,
    )

    @Suppress("UNCHECKED_CAST")
    /** IMPLICIT_TYPES 阶段暴露给通用 phase processor 的 transformer。 */
    override val transformer get() = implicitTypesTransformer as org.cangnova.cangjie.cfir.visitors.CfirTransformer<Nothing?>

    /** 以 context-independent 模式处理文件的隐式类型。 */
    override fun processFile(file: CfirFile) {
        implicitTypesTransformer.transformFile(file, ResolutionMode.ContextIndependent)
    }
}

/**
 * BODY_RESOLVE 阶段处理器。
 * 使用 [CfirBodyResolveTransformer] 对表达式级别的类型进行合成与检查。
 */
internal class CfirBodyResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.BODY_RESOLVE,
) {
    /** BODY_RESOLVE 阶段使用的 body resolve transformer。 */
    private val bodyResolveTransformer = CfirBodyResolveTransformer(
        session = session,
        scopeSession = scopeSession,
        returnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Default,
    )

    @Suppress("UNCHECKED_CAST")
    /** BODY_RESOLVE 阶段暴露给通用 phase processor 的 transformer。 */
    override val transformer get() = bodyResolveTransformer as org.cangnova.cangjie.cfir.visitors.CfirTransformer<Nothing?>

    /** 以 context-independent 模式处理文件 body。 */
    override fun processFile(file: CfirFile) {
        bodyResolveTransformer.transformFile(file, ResolutionMode.ContextIndependent)
    }
}
