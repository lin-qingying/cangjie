package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.test.directives.DiagnosticsDirectives
import org.cangnova.cangjie.test.model.TestModule

class DiagnosticsService(val testServices: TestServices) : TestService {
    companion object {
        private val severityNameMapping = mapOf(
            "infos" to Severity.INFO,
            "warnings" to Severity.WARNING,
            "errors" to Severity.ERROR,
        )
    }

    private val conditionsPerModule: MutableMap<TestModule, DiagnosticConditions> = mutableMapOf()

    private data class DiagnosticConditions(
        val allowedDiagnostics: Set<String>,
        val disabledDiagnostics: Set<String>,
        val severityMap: Map<Severity, Boolean>,
    )

    fun shouldRenderDiagnostic(module: TestModule, name: String, severity: Severity): Boolean {
        val conditions = conditionsPerModule.getOrPut(module) {
            computeDiagnosticConditionForModule(module)
        }

        val severityAllowed = conditions.severityMap.getOrDefault(severity, true)
        return if (severityAllowed) {
            name !in conditions.disabledDiagnostics || name in conditions.allowedDiagnostics
        } else {
            name in conditions.allowedDiagnostics
        }
    }

    private fun computeDiagnosticConditionForModule(module: TestModule): DiagnosticConditions {
        val diagnosticsInDirective = module.directives[DiagnosticsDirectives.DIAGNOSTICS]
        val enabledNames = mutableSetOf<String>()
        val disabledNames = mutableSetOf<String>()
        val severityMap = mutableMapOf<Severity, Boolean>()

        for (diagnosticInDirective in diagnosticsInDirective) {
            val enabled = when {
                diagnosticInDirective.startsWith("+") -> true
                diagnosticInDirective.startsWith("-") -> false
                else -> error("Incorrect diagnostics directive syntax. See ${DiagnosticsDirectives.DIAGNOSTICS.name}")
            }

            val name = diagnosticInDirective.substring(1)
            val severity = severityNameMapping[name]
            if (severity != null) {
                severityMap[severity] = enabled
            } else {
                val destination = if (enabled) enabledNames else disabledNames
                destination += name
            }
        }

        return DiagnosticConditions(
            allowedDiagnostics = enabledNames,
            disabledDiagnostics = disabledNames,
            severityMap = severityMap,
        )
    }
}

val TestServices.diagnosticsService: DiagnosticsService by TestServices.testServiceAccessor()
