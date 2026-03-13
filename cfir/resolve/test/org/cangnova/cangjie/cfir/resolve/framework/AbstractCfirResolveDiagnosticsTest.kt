package org.cangnova.cangjie.cfir.resolve.framework

import org.cangnova.cangjie.test.config.TestConfigurationBuilder
import org.cangnova.cangjie.test.runners.AbstractCangjieCompilerTest

abstract class AbstractCfirResolveDiagnosticsTest : AbstractCangjieCompilerTest() {
    override fun TestConfigurationBuilder.configuration() {
        useFrontendFacades({ CfirResolveDiagnosticsFacade() })
        useHandlers({ CfirResolveDiagnosticsHandler() })
    }

    protected fun runResolveDiagnosticsTest(testDataFilePath: String) {
        runTest(testDataFilePath)
    }
}

