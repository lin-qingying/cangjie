

package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory

/**
 * Contains all diagnostic factories that could be used in the current compilation
 */
class CjRegisteredDiagnosticFactoriesStorage {
    private val factories = mutableSetOf<BaseDiagnosticRendererFactory>()

    fun registerDiagnosticContainers(vararg containers: CjDiagnosticsContainer) {
        registerDiagnosticContainers(containers.toList())
    }

    fun registerDiagnosticContainers(containers: List<CjDiagnosticsContainer>) {
        this.factories += containers.map { it.getRendererFactory() }
    }

    val allDiagnosticFactories: List<AbstractCjDiagnosticFactory>
        get() = factories.flatMap { it.MAP.factories }
}

