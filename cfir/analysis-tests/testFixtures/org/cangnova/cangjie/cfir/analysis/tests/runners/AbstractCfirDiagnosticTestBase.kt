package org.cangnova.cangjie.cfir.analysis.tests.runners

import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.config.CfirTestDataConsistencyHandler
import org.cangnova.cangjie.test.config.configureDiagnosticTest
import org.cangnova.cangjie.test.directives.AdditionalFilesDirectives.SPEC_HELPERS
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.WITH_STDLIB
import org.cangnova.cangjie.test.frontend.CfirFailingTestSuppressor
import org.cangnova.cangjie.test.runners.AbstractCangjieCompilerTest
import org.cangnova.cangjie.test.services.sourceProviders.SpecHelpersSourceFilesProvider

/**
 * CFIR 诊断测试基类
 *
 * 对应 Kotlin K2 的 AbstractFirDiagnosticTestBase
 */
abstract class AbstractCfirDiagnosticTestBase(val parser: CfirParser) : AbstractCangjieCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        configureDiagnosticTest(parser)

    }
}

abstract class AbstractCfirPsiDiagnosticTest : AbstractCfirDiagnosticTestSpecBase(CfirParser.Psi)
abstract class AbstractCfirLightTreeDiagnosticsTest : AbstractCfirDiagnosticTestSpecBase(CfirParser.LightTree)

abstract class AbstractCfirDiagnosticTestSpecBase(parser: CfirParser) : AbstractCfirDiagnosticTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            baseCfirSpecDiagnosticTestConfiguration()
        }
    }
}
fun TestConfigurationBuilder.baseCfirSpecDiagnosticTestConfiguration() {
    defaultDirectives {
        +SPEC_HELPERS
        +WITH_STDLIB
    }

    useAdditionalSourceProviders(::SpecHelpersSourceFilesProvider)

    useAfterAnalysisCheckers(
        ::CfirTestDataConsistencyHandler,
        ::CfirFailingTestSuppressor,
    )

}
