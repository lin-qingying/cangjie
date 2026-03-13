package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirImplicitAwareBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirImplicitBodyResolveComputationSession
import org.cangnova.cangjie.cfir.resolve.body.CfirReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.body.CfirReturnTypeCalculatorWithJump
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * IMPLICIT_TYPES 阶段 processor。
 *
 * 使用 [CfirImplicitAwareBodyResolveTransformer] 进行隐式返回类型推断：
 * - 无显式返回类型的函数 → 从函数体最后一个表达式推断
 * - 无显式类型的属性/变量 → 从 initializer 推断
 * - 递归依赖保护（状态机：NotComputed → Computing → Computed）
 *
 * 参考 K2 FirImplicitTypeBodyResolveProcessor。
 */
internal class CfirImplicitTypesResolveProcessor(
    session: CfirSession,
    scopeSession: CfirScopeSession,
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
