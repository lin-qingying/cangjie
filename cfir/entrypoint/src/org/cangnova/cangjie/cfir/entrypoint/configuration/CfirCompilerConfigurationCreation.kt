package org.cangnova.cangjie.cfir.entrypoint.configuration

import org.cangnova.cangjie.cfir.diagnostics.CjRegisteredDiagnosticFactoriesStorage
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.create

/**
 * Creates a compiler configuration with the base services required by the
 * CFIR frontend pipeline.
 */
@JvmOverloads
fun CompilerConfiguration.Companion.createForCfirFrontend(
    messageCollector: MessageCollector = MessageCollector.NONE,
): CompilerConfiguration {
    return CompilerConfiguration.create(messageCollector = messageCollector).apply {
        initializeCfirFrontendConfiguration()
    }
}

/**
 * Ensures the base CFIR entrypoint services are available on an existing
 * configuration instance.
 */
fun CompilerConfiguration.initializeCfirFrontendConfiguration() {
    if (diagnosticFactoriesStorage == null) {
        diagnosticFactoriesStorage = CjRegisteredDiagnosticFactoriesStorage()
    }
}
