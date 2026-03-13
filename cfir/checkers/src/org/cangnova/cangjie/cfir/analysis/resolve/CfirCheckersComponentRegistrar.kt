package org.cangnova.cangjie.cfir.analysis.resolve

import org.cangnova.cangjie.cfir.analysis.CheckersComponent
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirBasicDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirBasicExpressionCheckers
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.transformers.CfirPhaseResolverRegistry
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * CHECKERS 阶段注册入口。
 *
 * 由 checkers 模块提供，负责向 session 注册 [CheckersComponent] 并向 resolve registry
 * 注册 CHECKERS 阶段处理器。调用方（entrypoint / 测试）在 resolve 注册完成后调用此方法。
 */
object CfirCheckersComponentRegistrar {
    fun register(
        session: CfirSession,
        registry: CfirPhaseResolverRegistry,
        diagnosticReporter: CfirDiagnosticReporter,
        scopeSession: CfirScopeSession = CfirScopeSession(),
    ) {
        val checkersComponent = CheckersComponent().apply {
            register(CfirBasicDeclarationCheckers)
            register(CfirBasicExpressionCheckers)
        }
        session.register(CheckersComponent::class, checkersComponent)

        registry.registerProcessor(
            CfirResolvePhase.CHECKERS,
            CfirCheckersResolveProcessor(diagnosticReporter, session, scopeSession),
        )
    }
}
