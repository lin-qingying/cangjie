package org.cangnova.cangjie.cfir.analysis.tests.runners

import org.cangnova.cangjie.cfir.analysis.tests.services.MacroConstructionEnvironmentConfigurator
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.config.CfirTestDataConsistencyHandler
import org.cangnova.cangjie.test.config.configureDiagnosticTest
import org.cangnova.cangjie.test.config.firHandlersStep
import org.cangnova.cangjie.test.directives.AdditionalFilesDirectives.SPEC_HELPERS
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.WITH_STDLIB
import org.cangnova.cangjie.test.directives.MacroConstructionDirectives
import org.cangnova.cangjie.test.frontend.CfirFailingTestSuppressor
import org.cangnova.cangjie.test.frontend.MacroExpandedCfirDumpHandler
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

/**
 * 宏端到端诊断测试基类。
 *
 * 该类只为 `testData/macro` 使用：测试数据中的 `macro package` 源文件
 * 会由 [MacroConstructionEnvironmentConfigurator] 自动识别并接入宏包编译请求。
 */
abstract class AbstractCfirLightTreeMacroDiagnosticsTest : AbstractCfirLightTreeDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useDirectives(MacroConstructionDirectives)
            useConfigurators(::MacroConstructionEnvironmentConfigurator)
            firHandlersStep {
                useHandlers(::MacroExpandedCfirDumpHandler)
            }
        }
    }
}

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
