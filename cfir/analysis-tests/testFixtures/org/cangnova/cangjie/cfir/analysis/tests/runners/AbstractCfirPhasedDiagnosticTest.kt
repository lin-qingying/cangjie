package org.cangnova.cangjie.cfir.analysis.tests.runners

import org.cangnova.cangjie.cfir.analysis.tests.services.CfirInlineDiagnosticsChecker
import org.cangnova.cangjie.cfir.analysis.tests.services.StructuredInlineDiagnosticsAssertionError
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.TargetBackend
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.config.TestPhaseDirectives
import org.cangnova.cangjie.test.config.TestPhase
import org.cangnova.cangjie.test.config.commonConfigurationForTest
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.CFIR_PARSER
import org.cangnova.cangjie.test.directives.LanguageSettingsDirectives
import org.cangnova.cangjie.test.frontend.CfirDefaultFacade
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.runners.AbstractCangjieCompilerWithTargetBackendTest
import org.cangnova.cangjie.test.services.impl.JUnit5Assertions

/**
 * CFIR phased diagnostics test base.
 *
 * Aligned with Kotlin K2 `AbstractFirPhasedDiagnosticTest`:
 * - declares phased defaults
 * - binds parser mode
 * - wires CFIR frontend phase facade
 */
abstract class AbstractCfirPhasedDiagnosticTest(
    val parser: CfirParser,
) : AbstractCangjieCompilerWithTargetBackendTest(TargetBackend.ANY) {

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        assertions = JUnit5Assertions
        useDirectives(LanguageSettingsDirectives, TestPhaseDirectives)

        defaultDirectives {
            TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE with TestPhase.BACKEND
            CFIR_PARSER with parser
        }

        commonConfigurationForTest(
            targetFrontend = FrontendKinds.CFIR,
            frontendFacade = ::CfirDefaultFacade,
        )


        enableMetaInfoHandler()
    }
}

open class AbstractPhasedDiagnosticLightTreeTest : AbstractCfirPhasedDiagnosticTest(CfirParser.LightTree)
open class AbstractPhasedDiagnosticPsiTest : AbstractCfirPhasedDiagnosticTest(CfirParser.Psi)

abstract class AbstractCfirStructuredPhasedDiagnosticTest(
    private val structuredParser: CfirParser,
) : AbstractCangjieCompilerWithTargetBackendTest(TargetBackend.ANY) {

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        useDirectives(LanguageSettingsDirectives, TestPhaseDirectives)

        defaultDirectives {
            TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE with TestPhase.BACKEND
            CFIR_PARSER with structuredParser
        }

        commonConfigurationForTest(
            targetFrontend = FrontendKinds.CFIR,
            frontendFacade = ::CfirDefaultFacade,
        )

        useAfterAnalysisCheckers(::CfirInlineDiagnosticsChecker)
    }

    override fun runTest(filePath: String) {
        try {
            super.runTest(filePath)
        } catch (error: AssertionError) {
            val structured = generateSequence(error as Throwable?) { it.cause }
                .filterIsInstance<StructuredInlineDiagnosticsAssertionError>()
                .firstOrNull()
            if (structured != null) {
                throw structured
            }
            throw error
        }
    }
}

open class AbstractStructuredPhasedDiagnosticLightTreeTest :
    AbstractCfirStructuredPhasedDiagnosticTest(CfirParser.LightTree)

open class AbstractStructuredPhasedDiagnosticPsiTest :
    AbstractCfirStructuredPhasedDiagnosticTest(CfirParser.Psi)
