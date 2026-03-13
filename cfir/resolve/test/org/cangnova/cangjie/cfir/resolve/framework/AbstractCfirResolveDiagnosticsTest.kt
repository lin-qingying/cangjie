package org.cangjie.cfir.resolve.framework

import org.cangjie.test.config.TestConfigurationBuilder
import org.cangjie.test.runners.AbstractCangjieCompilerTest

abstract class AbstractCfirResolveDiagnosticsTest : AbstractCangjieCompilerTest() {
    override fun TestConfigurationBuilder.configuration() {
        useFrontendFacades({ CfirResolveDiagnosticsFacade() })
        useHandlers({ CfirResolveDiagnosticsHandler() })
    }

    protected fun runResolveDiagnosticsTest(testDataFilePath: String) {
        runTest(testDataFilePath)
    }
}

