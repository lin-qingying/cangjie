package org.cangnova.cangjie.test.config

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices

/**
 * Compatibility hook retained for test infrastructure call sites.
 *
 * Multiplatform `dependsOn` closure is not supported in this repository,
 * so there are no additional source roots to inject here.
 */
fun CompilerConfiguration.addSourcesForDependsOnClosure(
    module: TestModule,
    testServices: TestServices,
) {
    @Suppress("UNUSED_VARIABLE")
    val ignored = module to testServices
}
