package org.cangjie.cfir.providers

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.diagnostics.CfirDiagnosticCollector
import org.cangjie.cfir.diagnostics.CfirDiagnosticReporter
import org.cangjie.cfir.resolve.CfirLegacyResolveComponentsRegistrar
import org.cangjie.cfir.resolve.CfirPhaseResolverRegistry
import org.cangjie.cfir.resolve.CfirResolveComponentsRegistrar
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CfirResolveProviderPipelineTest {
    @Test
    fun formalPipelineExposesBuiltinSymbolProvider() {
        val session = createSession()
        val registry = CfirPhaseResolverRegistry()
        val diagnostics = CfirDiagnosticCollector()
        registerBaseComponents(session, registry, diagnostics)

        CfirResolveComponentsRegistrar.register(session, registry, diagnostics)

        assertTrue(session.symbolProvider.hasPackage(StandardNames.BASIC_PACKAGE_FQ_NAME))
        assertNotNull(
            session.symbolProvider.getClassLikeSymbolByClassId(
                org.cangnova.cangjie.name.ClassId(
                    StandardNames.BASIC_PACKAGE_FQ_NAME,
                    Name.identifier("Int64"),
                ),
            ),
        )
    }

    @Test
    fun legacyPipelineKeepsSymbolProviderEmpty() {
        val session = createSession()
        val registry = CfirPhaseResolverRegistry()
        val diagnostics = CfirDiagnosticCollector()
        registerBaseComponents(session, registry, diagnostics)

        CfirLegacyResolveComponentsRegistrar.register(session, registry, diagnostics)

        assertFalse(session.symbolProvider.hasPackage(StandardNames.BASIC_PACKAGE_FQ_NAME))
        assertNull(
            session.symbolProvider.getClassLikeSymbolByClassId(
                org.cangnova.cangjie.name.ClassId(
                    StandardNames.BASIC_PACKAGE_FQ_NAME,
                    Name.identifier("Int64"),
                ),
            ),
        )
    }

    private fun createSession(): CfirSession = object : CfirSession(CfirSession.Kind.Source) {}

    private fun registerBaseComponents(
        session: CfirSession,
        registry: CfirPhaseResolverRegistry,
        diagnostics: CfirDiagnosticCollector,
    ) {
        session.register(CfirModuleData::class, CfirModuleData(Name.identifier("provider-pipeline-test")))
        session.register(CfirPhaseResolverRegistry::class, registry)
        session.register(CfirDiagnosticReporter::class, diagnostics)
        session.register(CfirDiagnosticCollector::class, diagnostics)
    }
}
