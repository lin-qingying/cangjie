package org.cangnova.cangjie.test.builders

import org.cangnova.cangjie.test.NonGroupingTestRunner


inline fun nonGroupingPhaseTestRunner(testDataPath: String, crossinline init: TestConfigurationBuilder.() -> Unit): NonGroupingTestRunner {
    return NonGroupingTestRunner(testConfiguration(testDataPath, init))
}
