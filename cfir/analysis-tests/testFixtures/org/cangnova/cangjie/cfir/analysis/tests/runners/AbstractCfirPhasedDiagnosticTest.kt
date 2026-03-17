package org.cangnova.cangjie.cfir.analysis.tests.runners

import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.TargetBackend
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.directives.LanguageSettingsDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.frontend.CfirDefaultFacade
import org.cangnova.cangjie.test.runners.AbstractCangjieCompilerWithTargetBackendTest

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
        useDirectives(LanguageSettingsDirectives, CfirPhasedDiagnosticDirectives)

        defaultDirectives {
            put(CfirPhasedDiagnosticDirectives.LATEST_PHASE_IN_PIPELINE, TestPhase.BACKEND)
            put(CfirPhasedDiagnosticDirectives.CFIR_PARSER, parser)
            put(LanguageSettingsDirectives.LANGUAGE, "+EnableDfaWarningsInC2")
        }

        // Current Cangjie phased diagnostics pipeline is frontend-only for now.
        // Keep the shape aligned with K2 phased configuration and extend with
        // converter/backend facades when those artifacts are available.
        facadeStep(::CfirDefaultFacade)

        enableMetaInfoHandler()
    }
}

open class AbstractPhasedDiagnosticLightTreeTest : AbstractCfirPhasedDiagnosticTest(CfirParser.LightTree)
open class AbstractPhasedDiagnosticPsiTest : AbstractCfirPhasedDiagnosticTest(CfirParser.Psi)

enum class TestPhase {
    FRONTEND,
    BACKEND,
}

object CfirPhasedDiagnosticDirectives : SimpleDirectivesContainer() {
    val LATEST_PHASE_IN_PIPELINE by enumDirective<TestPhase>(
        description = "Latest phase that should be executed by phased diagnostics pipeline."
    )

    val CFIR_PARSER by enumDirective<CfirParser>(
        description = "Parser mode used by phased diagnostics tests."
    )

    val SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE by stringDirective(
        description = "Suppresses failures in without-alias-expansion diagnostics mode."
    )
}
