

package org.cangnova.cangjie.analysis.low.level.api.cfir.state

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

interface LLDiagnosticProvider {
    /**
     * Returns all compiler diagnostics for the [file], matching the [filter].
     */
    fun collectDiagnostics(file: CjFile, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic>

    /**
     * Returns all compiler diagnostics for the specific [element], matching the [filter].
     * This function is not recursive; diagnostics for nested elements are not returned.
     */
    fun getDiagnostics(element: CjElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic>
}

internal object LLEmptyDiagnosticProvider : LLDiagnosticProvider {
    override fun collectDiagnostics(file: CjFile, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        return emptyList()
    }

    override fun getDiagnostics(element: CjElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        return emptyList()
    }
}

internal class LLSourceDiagnosticProvider(
    private val moduleProvider: LLModuleProvider,
    private val sessionProvider: LLSessionProvider
) : LLDiagnosticProvider {
    override fun collectDiagnostics(file: CjFile, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        val module = moduleProvider.getModule(file)
        val moduleComponents = sessionProvider.getResolvableSession(module).moduleComponents
        return moduleComponents.diagnosticsCollector.collectDiagnosticsForFile(file, filter)
    }

    override fun getDiagnostics(element: CjElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        val module = moduleProvider.getModule(element)
        val moduleComponents = sessionProvider.getResolvableSession(module).moduleComponents
        return moduleComponents.diagnosticsCollector.getDiagnosticsFor(element, filter)
    }
}
