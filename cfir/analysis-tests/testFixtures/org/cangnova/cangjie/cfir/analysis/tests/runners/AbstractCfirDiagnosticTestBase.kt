package org.cangnova.cangjie.cfir.analysis.tests.runners

import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.runners.AbstractCangjieCompilerTest

/**
 * CFIR 诊断测试基类
 *
 * 对应 Kotlin K2 的 AbstractFirDiagnosticTestBase
 */
abstract class AbstractCfirDiagnosticTestBase(val parser: CfirParser) : AbstractCangjieCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        // TODO: 配置诊断测试
    }
}

abstract class AbstractCfirPsiDiagnosticTest : AbstractCfirDiagnosticTestBase(CfirParser.Psi)
abstract class AbstractCfirLightTreeDiagnosticsTest : AbstractCfirDiagnosticTestBase(CfirParser.LightTree)
