package org.cangjie.cfir.session

import org.cangjie.cfir.diagnostics.CfirDiagnosticCollector
import org.cangjie.cfir.providers.CfirExtendProvider
import org.cangjie.cfir.providers.CfirProvider
import org.cangjie.cfir.providers.CfirSymbolProvider
import org.cangjie.cfir.resolve.CfirPhaseResolverRegistry
import org.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore

val CfirSession.symbolProvider: CfirSymbolProvider by CfirSession.sessionComponentAccessor()

val CfirSession.cfirProvider: CfirProvider by CfirSession.sessionComponentAccessor()

val CfirSession.extendProvider: CfirExtendProvider by CfirSession.sessionComponentAccessor()

val CfirSession.phaseResolverRegistry: CfirPhaseResolverRegistry by CfirSession.sessionComponentAccessor()

val CfirSession.diagnosticCollector: CfirDiagnosticCollector by CfirSession.sessionComponentAccessor()

val CfirSession.importBindingStore: CfirImportBindingStore by CfirSession.sessionComponentAccessor()

val CfirSession.importBindingStoreOrNull: CfirImportBindingStore? by CfirSession.nullableSessionComponentAccessor()

val CfirSession.superTypeGraphStore: CfirSuperTypeGraphStore by CfirSession.sessionComponentAccessor()

val CfirSession.superTypeGraphStoreOrNull: CfirSuperTypeGraphStore? by CfirSession.nullableSessionComponentAccessor()
