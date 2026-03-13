package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirResolutionFacadeImpl
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirResolutionFacadeService
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirResolveFacade
import org.cangnova.cangjie.cfir.analysis.resolve.CfirCheckersComponentRegistrar
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirPackageDirective
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.transformers.CfirResolveComponentsRegistrar
import org.cangnova.cangjie.cfir.resolve.transformers.CfirPhaseResolverRegistry
import org.cangnova.cangjie.cfir.session.CfirSession
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

class AnalysisApiSurfaceTest {
    @Test
    fun cfirSurfaceTypesAreAvailable() {
        assertNotNull(CaCfirSession::class.java)
        assertNotNull(CaCfirResolutionFacadeService::class.java)
    }

    @Test
    fun cfirPluginDescriptorIsAvailable() {
        val resource = javaClass.classLoader.getResource(
            "META-INF/analysis-api/cangjie-analysis-api-cfir.xml"
        )
        assertNotNull(resource, "CFIR analysis-api plugin descriptor should be available on test classpath")
    }

    @Test
    fun resolveFacadeConsumesPhaseProgressionAcrossModules() {
        val moduleA = object : CaModule {
            override val name: String = "moduleA"
            override val project
                get() = error("project is not used in this test")
        }
        val moduleB = object : CaModule {
            override val name: String = "moduleB"
            override val project
                get() = error("project is not used in this test")
        }

        val diagnosticsA = resolveSingleClassToCheckers(moduleA, "A")
        val diagnosticsB = resolveSingleClassToCheckers(moduleB, "B")

        assertTrue(diagnosticsA.none { it.severity.name == "ERROR" })
        assertTrue(diagnosticsB.none { it.severity.name == "ERROR" })
    }

    private fun resolveSingleClassToCheckers(module: CaModule, className: String) =
        createResolveFacade(module).resolveTo(
            file = CfirFile(
                moduleData = CfirModuleData(Name.identifier(module.name)),
                name = "$className.cj",
                packageDirective = CfirPackageDirective(FqName.ROOT),
                declarations = mutableListOf(
                    CfirClass(
                        moduleData = CfirModuleData(Name.identifier(module.name)),
                        name = Name.identifier(className),
                        classKind = CfirClassKind.CLASS,
                    ),
                ),
            ),
            targetPhase = CfirResolvePhase.CHECKERS,
        ).diagnostics

    private fun createResolveFacade(module: CaModule): CaCfirResolveFacade {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val diagnostics = CfirDiagnosticCollector()
        val moduleData = CfirModuleData(Name.identifier(module.name))
        val phaseResolverRegistry = CfirPhaseResolverRegistry()
        session.register(CfirModuleData::class, moduleData)
        session.register(CfirPhaseResolverRegistry::class, phaseResolverRegistry)
        session.register(CfirDiagnosticReporter::class, diagnostics)
        session.register(CfirDiagnosticCollector::class, diagnostics)
        CfirResolveComponentsRegistrar.register(session, phaseResolverRegistry, diagnostics)
        CfirCheckersComponentRegistrar.register(session, phaseResolverRegistry, diagnostics)
        val resolutionFacade = CaCfirResolutionFacadeImpl(module, session, diagnostics)
        return CaCfirResolveFacade(resolutionFacade)
    }
}

