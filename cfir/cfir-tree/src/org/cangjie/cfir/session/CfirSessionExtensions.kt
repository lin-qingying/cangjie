package org.cangjie.cfir.session

import org.cangjie.cfir.diagnostics.CfirDiagnosticCollector
import org.cangjie.cfir.providers.CfirExtendProvider
import org.cangjie.cfir.providers.CfirProvider
import org.cangjie.cfir.providers.CfirSymbolProvider
import org.cangjie.cfir.resolve.CfirPhaseResolverRegistry

val CfirSession.symbolProvider: CfirSymbolProvider by CfirSession.sessionComponentAccessor()

val CfirSession.cfirProvider: CfirProvider by CfirSession.sessionComponentAccessor()

val CfirSession.extendProvider: CfirExtendProvider by CfirSession.sessionComponentAccessor()

val CfirSession.phaseResolverRegistry: CfirPhaseResolverRegistry by CfirSession.sessionComponentAccessor()

val CfirSession.diagnosticCollector: CfirDiagnosticCollector by CfirSession.sessionComponentAccessor()
