package org.cangjie.cfir.session

import org.cangjie.cfir.resolve.transformers.CfirPhaseResolverRegistry
import org.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangjie.cfir.resolve.services.CfirLazyDeclarationResolver
import org.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore

val CfirSession.phaseResolverRegistry: CfirPhaseResolverRegistry by CfirSession.sessionComponentAccessor()

val CfirSession.lazyDeclarationResolver: CfirLazyDeclarationResolver by CfirSession.sessionComponentAccessor()

val CfirSession.lazyDeclarationResolverOrNull: CfirLazyDeclarationResolver? by CfirSession.nullableSessionComponentAccessor()

val CfirSession.diagnosticReporter: CfirDiagnosticReporter by CfirSession.sessionComponentAccessor()
val CfirSession.diagnosticCollector: CfirDiagnosticCollector by CfirSession.sessionComponentAccessor()

val CfirSession.importBindingStore: CfirImportBindingStore by CfirSession.sessionComponentAccessor()

val CfirSession.importBindingStoreOrNull: CfirImportBindingStore? by CfirSession.nullableSessionComponentAccessor()

val CfirSession.superTypeGraphStore: CfirSuperTypeGraphStore by CfirSession.sessionComponentAccessor()

val CfirSession.superTypeGraphStoreOrNull: CfirSuperTypeGraphStore? by CfirSession.nullableSessionComponentAccessor()
