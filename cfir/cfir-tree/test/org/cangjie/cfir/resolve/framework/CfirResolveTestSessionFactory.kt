package org.cangjie.cfir.resolve.framework

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.diagnostics.CfirDiagnosticCollector
import org.cangjie.cfir.diagnostics.CfirDiagnosticReporter
import org.cangjie.cfir.resolve.CfirResolveComponentsRegistrar
import org.cangjie.cfir.resolve.CfirPhaseResolverRegistry
import org.cangjie.cfir.session.CfirSession
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


