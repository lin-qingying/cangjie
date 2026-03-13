package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.providers.CfirResolveProviderPipeline
import org.cangnova.cangjie.cfir.resolve.services.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangnova.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession

/**
 * CFIR resolve 组件注册入口。
 *
 * 仅注册 resolve 阶段自身需要的服务和处理器（IMPORTS → BODY_RESOLVE）。
 * CHECKERS 阶段由 checkers 模块独立注册（见 [org.cangnova.cangjie.cfir.analysis.resolve] 包）。
 */
object CfirResolveComponentsRegistrar {
    fun register(
        session: CfirSession,
        registry: CfirPhaseResolverRegistry,
        diagnosticReporter: CfirDiagnosticReporter,
    ) {
        CfirResolveProviderPipeline
            .formalDefaults()
            .registerInto(session)
        session.register(CfirLazyDeclarationResolver::class, CfirLazyDeclarationResolver())
        session.register(CfirImportBindingStore::class, CfirImportBindingStore())
        session.register(CfirSuperTypeGraphStore::class, CfirSuperTypeGraphStore())
        registerResolveProcessors(registry, diagnosticReporter, session, CfirScopeSession())
    }
}

object CfirLegacyResolveComponentsRegistrar {
    fun register(
        session: CfirSession,
        registry: CfirPhaseResolverRegistry,
        diagnosticReporter: CfirDiagnosticReporter,
    ) {
        CfirResolveProviderPipeline
            .legacyCompatibleDefaults()
            .registerInto(session)
        session.register(CfirLazyDeclarationResolver::class, CfirLazyDeclarationResolver())
        session.register(CfirImportBindingStore::class, CfirImportBindingStore())
        session.register(CfirSuperTypeGraphStore::class, CfirSuperTypeGraphStore())
        registerResolveProcessors(registry, diagnosticReporter, session, CfirScopeSession())
    }
}
