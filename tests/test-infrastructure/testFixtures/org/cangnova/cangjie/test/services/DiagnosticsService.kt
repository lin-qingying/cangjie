package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.test.directives.DiagnosticsDirectives
import org.cangnova.cangjie.test.model.TestModule

/**
 * 表示 `DiagnosticsService`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class DiagnosticsService(val testServices: TestServices) : TestService {
    companion object {
        private val severityNameMapping = mapOf(
            "infos" to Severity.INFO,
            "warnings" to Severity.WARNING,
            "errors" to Severity.ERROR,
        )
    }

    /**
     * 保存 `conditionsPerModule`，供测试服务在测试执行期间读取或传递。
     */
    private val conditionsPerModule: MutableMap<TestModule, DiagnosticConditions> = mutableMapOf()

    /**
     * 表示 `DiagnosticConditions`，承载测试服务中的配置数据、测试产物或处理步骤。
     */
    private data class DiagnosticConditions(
        /**
         * 保存 `allowedDiagnostics`，供测试服务在测试执行期间读取或传递。
         */
        val allowedDiagnostics: Set<String>,
        /**
         * 保存 `disabledDiagnostics`，供测试服务在测试执行期间读取或传递。
         */
        val disabledDiagnostics: Set<String>,
        /**
         * 保存 `severityMap`，供测试服务在测试执行期间读取或传递。
         */
        val severityMap: Map<Severity, Boolean>,
    )

    /**
     * 执行 `shouldRenderDiagnostic` 对应的测试服务流程，维持测试框架的阶段契约。
     */
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

    /**
     * 提供 `computeDiagnosticConditionForModule` 对应的测试服务流程，维持测试框架的阶段契约。
     */
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

/**
 * 保存 `TestServices.diagnosticsService`，供测试服务在测试执行期间读取或传递。
 */
val TestServices.diagnosticsService: DiagnosticsService by TestServices.testServiceAccessor()
