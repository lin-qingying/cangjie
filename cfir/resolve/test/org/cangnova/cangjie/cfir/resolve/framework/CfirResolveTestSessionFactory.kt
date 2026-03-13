package org.cangnova.cangjie.cfir.resolve.framework

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.resolve.transformers.CfirResolveComponentsRegistrar
import org.cangnova.cangjie.cfir.resolve.transformers.CfirPhaseResolverRegistry
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.Name

data class CfirResolveTestSessionContext(
    val session: CfirSession,
    val phaseRegistry: CfirPhaseResolverRegistry,
    val moduleData: CfirModuleData,
    val diagnostics: CfirDiagnosticCollector,
)

fun createCfirResolveTestSessionContext(moduleName: String): CfirResolveTestSessionContext {
    val session = object : CfirSession(CfirSession.Kind.Source) {}
    val diagnostics = CfirDiagnosticCollector()
    val moduleData = CfirModuleData(Name.identifier(moduleName))
    val phaseRegistry = CfirPhaseResolverRegistry()

    session.register(CfirModuleData::class, moduleData)
    session.register(CfirPhaseResolverRegistry::class, phaseRegistry)
    session.register(CfirDiagnosticReporter::class, diagnostics)
    session.register(CfirDiagnosticCollector::class, diagnostics)
    CfirResolveComponentsRegistrar.register(session, phaseRegistry, diagnostics)

    return CfirResolveTestSessionContext(
        session = session,
        phaseRegistry = phaseRegistry,
        moduleData = moduleData,
        diagnostics = diagnostics,
    )
}


