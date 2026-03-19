package org.cangnova.cangjie.cfir.diagnostics.rendering

import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactoryToRendererMap
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRenderer

fun interface DiagnosticRendererFactory {
    operator fun invoke(diagnostic: CjDiagnostic): CjDiagnosticRenderer?
}

abstract class BaseDiagnosticRendererFactory : DiagnosticRendererFactory {
    override operator fun invoke(diagnostic: CjDiagnostic): CjDiagnosticRenderer? {
        val factory = diagnostic.factory
        @Suppress("UNCHECKED_CAST")
        return MAP[factory]
    }

    abstract val MAP: CjDiagnosticFactoryToRendererMap
}

abstract class BaseSourcelessDiagnosticRendererFactory : BaseDiagnosticRendererFactory() {
    companion object {
        const val MESSAGE_PLACEHOLDER: String = "{0}"
    }
}


