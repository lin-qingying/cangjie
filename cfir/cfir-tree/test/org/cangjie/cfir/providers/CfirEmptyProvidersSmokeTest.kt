package org.cangjie.cfir.providers

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.diagnostics.CfirDiagnosticCollector
import org.cangjie.cfir.diagnostics.CfirDiagnosticReporter
import org.cangjie.cfir.resolve.CfirResolveComponentsRegistrar
import org.cangjie.cfir.resolve.CfirPhaseResolverRegistry
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.session.cfirProvider
import org.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CfirEmptyProvidersSmokeTest {
    @Test
    fun emptyProvidersAreRegisteredAndQueryable() {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = CfirModuleData(Name.identifier("<provider-test>"))
        val diagnostics = CfirDiagnosticCollector()
        val phaseRegistry = CfirPhaseResolverRegistry()
        session.register(CfirModuleData::class, moduleData)
        session.register(CfirPhaseResolverRegistry::class, phaseRegistry)
        session.register(CfirDiagnosticReporter::class, diagnostics)
        session.register(CfirDiagnosticCollector::class, diagnostics)
        CfirResolveComponentsRegistrar.register(session, phaseRegistry, diagnostics)

        assertTrue(session.cfirProvider.getCfirFilesByPackage(FqName.ROOT).isEmpty())
        assertNull(session.cfirProvider.getClassByClassId(ClassId(FqName.ROOT, Name.identifier("Missing"))))
        assertTrue(
            session.symbolProvider.hasPackage(FqName.ROOT) ||
                session.symbolProvider.hasPackage(StandardNames.BASIC_PACKAGE_FQ_NAME),
        )
        assertNull(session.symbolProvider.getClassLikeSymbolByClassId(ClassId(FqName.ROOT, Name.identifier("Missing"))))
    }
}

