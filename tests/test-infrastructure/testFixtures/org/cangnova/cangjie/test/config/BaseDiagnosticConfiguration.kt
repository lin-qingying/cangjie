package org.cangnova.cangjie.test.config

import org.cangnova.cangjie.LanguageVersions
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.Constructor
import org.cangnova.cangjie.test.builders.CFIR_HANDLERS_STEP_NAME
import org.cangnova.cangjie.test.builders.TestConfigurationBuilderBase
import org.cangnova.cangjie.test.builders.TestStepBuilder
import org.cangnova.cangjie.test.directives.AdditionalFilesDirectives
import org.cangnova.cangjie.test.directives.CangjieTestDirectives
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.CFIR_PARSER
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.DISABLE_WITH_PARSER
import org.cangnova.cangjie.test.directives.LanguageSettingsDirectives
import org.cangnova.cangjie.test.directives.LanguageSettingsDirectives.ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
import org.cangnova.cangjie.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.cangnova.cangjie.test.directives.LanguageSettingsDirectives.LANGUAGE_VERSION
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleOrZeroValue
import org.cangnova.cangjie.test.frontend.CfirDiagnosticsHandler
import org.cangnova.cangjie.test.frontend.CfirFrontendFacade
import org.cangnova.cangjie.test.frontend.CfirInferenceLogsHandler
import org.cangnova.cangjie.test.frontend.CfirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar
import org.cangnova.cangjie.test.frontend.CfirOutputArtifact
import org.cangnova.cangjie.test.frontend.CfirResolveContractViolationErrorHandler
import org.cangnova.cangjie.test.frontend.CfirResolvedTypesVerifier
import org.cangnova.cangjie.test.frontend.CfirScopeDumpHandler
import org.cangnova.cangjie.test.frontend.PsiLightTreeMetaInfoProcessor
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.model.DependencyKind
import org.cangnova.cangjie.test.model.FrontendFacade
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.services.CommonEnvironmentConfigurator
import org.cangnova.cangjie.test.services.CompilationStage
import org.cangnova.cangjie.test.services.MetaTestConfigurator
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.moduleStructure
import org.cangnova.cangjie.test.services.service

/**
 * General test configuration for CFIR-based diagnostic tests.
 */
fun TestConfigurationBuilder.configureDiagnosticTest(parser: CfirParser) {
    baseCfirDiagnosticTestConfiguration()
    enableLazyResolvePhaseChecking()
    configureCfirParser(parser)

    useAdditionalService(::LibraryProvider)
    useMetaTestConfigurators(::CfirSpecificParserSuppressor)
}

/**
 * Setups base configuration for diagnostics tests:
 * - CFIR frontend step
 * - source dependency kind between modules
 * - common diagnostic directives/configurators
 */
fun TestConfigurationBuilder.baseCfirDiagnosticTestConfiguration(
    @Suppress("unused") baseDir: String = ".",
    frontendFacade: Constructor<FrontendFacade<CfirOutputArtifact>> = ::CfirFrontendFacade,
    testDataConsistencyHandler: Constructor<AfterAnalysisChecker> = ::CfirTestDataConsistencyHandler,
) {
    globalDefaults {
        frontend = FrontendKinds.CFIR
        dependencyKind = DependencyKind.Source
    }

    defaultDirectives {
        LANGUAGE + "+EnableDfaWarnings"
    }

    enableMetaInfoHandler()

    useDirectives(
        CfirDiagnosticsDirectives,
        LanguageSettingsDirectives,
        AdditionalFilesDirectives,
        CangjieTestDirectives,
        TestPhaseDirectives,
    )

    useConfigurators(
        ::CommonEnvironmentConfigurator,
    )

    // Reserved hook for diagnostic helper sources.
    useAdditionalSourceProviders()

    facadeStep(frontendFacade)
    firHandlersStep {
        setupHandlersForDiagnosticTest()
    }



    useMetaInfoProcessors(::PsiLightTreeMetaInfoProcessor)

    configureCommonDiagnosticTestPaths(testDataConsistencyHandler)
}
fun TestStepBuilder.HandlersStepBuilder.NonGroupingPhase<CfirOutputArtifact, FrontendKinds.CFIR>.setupHandlersForDiagnosticTest() {
    useHandlers(
        ::CfirDiagnosticsHandler,
//        ::CfirDumpHandler,
//        ::CfirCfgDumpHandler,
//        ::CfirVFirDumpHandler,
        ::CfirInferenceLogsHandler,
//        ::CfirCfgConsistencyHandler,
        ::CfirResolvedTypesVerifier,
        ::CfirScopeDumpHandler,
    )
}
/**
 * Compatibility hook retained for old call sites.
 * This repository executes diagnostics in a single CFIR pipeline.
 */
fun TestConfigurationBuilder.configurationForClassicAndCfirTestsAlongside(
    testDataConsistencyHandler: Constructor<AfterAnalysisChecker> = ::CfirTestDataConsistencyHandler,
) {
    useAfterAnalysisCheckers(testDataConsistencyHandler)
}
inline fun TestConfigurationBuilder.firHandlersStep(
    noinline init: TestStepBuilder.HandlersStepBuilder.NonGroupingPhase<CfirOutputArtifact, FrontendKinds.CFIR>.() -> Unit = {}
) {
    namedHandlersStep(CFIR_HANDLERS_STEP_NAME, FrontendKinds.CFIR, CompilationStage.FIRST, init)
}
/**
 * Sets path-scoped defaults for diagnostics tests.
 */
fun TestConfigurationBuilder.configureCommonDiagnosticTestPaths(
    testDataConsistencyHandler: Constructor<AfterAnalysisChecker> = ::CfirTestDataConsistencyHandler,
) {
    @Suppress("UNUSED_VARIABLE")
    val ignored = testDataConsistencyHandler
}

/**
 * Runs diagnostics tests with the latest available Cangjie language version
 * supported by this repository.
 */
fun TestConfigurationBuilder.configurationForTestWithLatestLanguageVersion() {
    defaultDirectives {
        LANGUAGE_VERSION with LanguageVersions.LATEST_STABLE.toString()
        +ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
    }
    useAfterAnalysisCheckers(::CfirTestDataConsistencyHandler)
}

/**
 * Hook for lazy-resolve phase contracts. The current CFIR test setup has no dedicated
 * lazy resolve phase checker yet, so this method intentionally keeps a stable API surface.
 */
fun TestConfigurationBuilder.enableLazyResolvePhaseChecking() {
    useAdditionalServices(service(::CfirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar))
    useAfterAnalysisCheckers(::CfirResolveContractViolationErrorHandler, insertAtFirst = true)
}

fun TestConfigurationBuilderBase<*, *>.configureCfirParser(parser: CfirParser) {
    defaultDirectives {
        CFIR_PARSER with parser
    }
}

/**
 * Ensures testdata-variant consistency hooks can be attached without making diagnostics
 * configuration depend on runner-specific handlers.
 */
class CfirTestDataConsistencyHandler(
    testServices: TestServices,
) : AfterAnalysisChecker(testServices)

/**
 * Skip tests when they are explicitly disabled for the currently selected parser.
 */
class CfirSpecificParserSuppressor(
    testServices: TestServices,
) : MetaTestConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CfirDiagnosticsDirectives)

    override fun shouldSkipTest(): Boolean {
        val allDirectives = testServices.moduleStructure.allDirectives
        val disabledParser = allDirectives.singleOrZeroValue(DISABLE_WITH_PARSER) ?: return false
        val activeParser = allDirectives[CFIR_PARSER].lastOrNull() ?: return false
        return disabledParser == activeParser
    }
}

/**
 * Placeholder service for diagnostic test configurations that need to pass a library provider
 * handle through [TestServices].
 */
class LibraryProvider(
    @Suppress("UNUSED_PARAMETER") testServices: TestServices,
) : TestService
