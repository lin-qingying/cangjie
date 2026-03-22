package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirImplicitAwareBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirImplicitBodyResolveComputationSession
import org.cangnova.cangjie.cfir.resolve.body.CfirReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.body.CfirReturnTypeCalculatorWithJump
import org.cangnova.cangjie.cfir.ScopeSession
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
    private val computationSession = CfirImplicitBodyResolveComputationSession()
    private val returnTypeCalculator = CfirReturnTypeCalculatorWithJump(session, scopeSession, computationSession)

    private val implicitTypesTransformer = CfirImplicitAwareBodyResolveTransformer(
        session = session,
        scopeSession = scopeSession,
        implicitBodyResolveComputationSession = computationSession,
        phase = CfirResolvePhase.IMPLICIT_TYPES,
        implicitTypeOnly = true,
        returnTypeCalculator = returnTypeCalculator,
    )

    @Suppress("UNCHECKED_CAST")
    override val transformer get() = implicitTypesTransformer as org.cangnova.cangjie.cfir.visitors.CfirTransformer<Nothing?>

    override fun processFile(file: CfirFile) {
        implicitTypesTransformer.transformFile(file, CfirResolutionMode.ContextIndependent)
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
    private val bodyResolveTransformer = CfirBodyResolveTransformer(
        session = session,
        scopeSession = scopeSession,
        returnTypeCalculator = CfirReturnTypeCalculatorForFullBodyResolve.Default,
    )

    @Suppress("UNCHECKED_CAST")
    override val transformer get() = bodyResolveTransformer as org.cangnova.cangjie.cfir.visitors.CfirTransformer<Nothing?>

    override fun processFile(file: CfirFile) {
        bodyResolveTransformer.transformFile(file, CfirResolutionMode.ContextIndependent)
    }
}

