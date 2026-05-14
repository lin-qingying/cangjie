package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 注册 ordinary resolve phase 处理器。
 *
 * 宏展开已在 baseline 第 1 节描述的 construction step 中完成
 * （由 `MacroConstructionService` 与 `recordExpandedRawFilesOnce` 处理），
 * 因此该函数注册的 phase 序列固定为：
 *
 * ```
 * IMPORTS → SUPER_TYPES → TYPES → STATUS → EXTENSIONS → IMPLICIT_TYPES → BODY_RESOLVE
 * ```
 */
fun registerResolveProcessors(
    registry: CfirPhaseResolverRegistry,
    diagnosticReporter: CfirDiagnosticReporter,
    session: CfirSession,
    scopeSession: ScopeSession = ScopeSession(),
) {
    registry.registerProcessor(
        CfirResolvePhase.IMPORTS,
        CfirImportResolveProcessor(diagnosticReporter, session, scopeSession),
    )
    registry.registerProcessor(
        CfirResolvePhase.SUPER_TYPES,
        CfirSupertypeResolverProcessor(session, scopeSession),
    )
    registry.registerProcessor(CfirResolvePhase.TYPES, CfirTypeResolveProcessor(session, scopeSession))
    registry.registerProcessor(
        CfirResolvePhase.STATUS,
        CfirStatusResolveProcessor(session, scopeSession),
    )
    registry.registerProcessor(
        CfirResolvePhase.EXTENSIONS,
        CfirExtensionsResolveProcessor(session, scopeSession),
    )
    registry.registerProcessor(
        CfirResolvePhase.IMPLICIT_TYPES,
        CfirImplicitTypesResolveProcessor(session, scopeSession),
    )
    registry.registerProcessor(CfirResolvePhase.BODY_RESOLVE, CfirBodyResolveProcessor(session, scopeSession))
}
