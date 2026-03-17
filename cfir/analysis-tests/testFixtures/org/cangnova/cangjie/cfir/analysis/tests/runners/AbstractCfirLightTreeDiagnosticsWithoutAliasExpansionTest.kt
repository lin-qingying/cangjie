package org.cangnova.cangjie.cfir.analysis.tests.runners

import org.cangnova.cangjie.cfir.analysis.tests.services.CfirWithoutAliasExpansionTestSuppressor
import org.cangnova.cangjie.cfir.analysis.tests.services.OnlyTestsWithTypeAliasesMetaConfigurator
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.directives.ConfigurationDirectives

/**
 * CFIR LightTree 诊断测试（禁用类型别名展开）
 *
 * 对应 Kotlin K2 的 `AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest`
 */
open class AbstractCfirLightTreeDiagnosticsWithoutAliasExpansionTest :
    AbstractCfirPhasedDiagnosticTest(CfirParser.LightTree) {

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +ConfigurationDirectives.DISABLE_TYPEALIAS_EXPANSION
            }

            useAfterAnalysisCheckers(::CfirWithoutAliasExpansionTestSuppressor)
            useMetaTestConfigurators(::OnlyTestsWithTypeAliasesMetaConfigurator)
        }
    }
}
