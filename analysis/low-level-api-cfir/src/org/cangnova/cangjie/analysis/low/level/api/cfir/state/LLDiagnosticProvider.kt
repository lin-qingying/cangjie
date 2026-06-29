

package org.cangnova.cangjie.analysis.low.level.api.cfir.state

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * low-level API 诊断查询入口。
 */
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

/**
 * 不产生任何诊断的空 provider。
 */
internal object LLEmptyDiagnosticProvider : LLDiagnosticProvider {
    /**
     * 对空 provider 来说文件诊断恒为空。
     */
    override fun collectDiagnostics(file: CjFile, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        return emptyList()
    }

    /**
     * 对空 provider 来说元素诊断恒为空。
     */
    override fun getDiagnostics(element: CjElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        return emptyList()
    }
}

/**
 * 基于源码 session diagnostics collector 的诊断 provider。
 */
internal class LLSourceDiagnosticProvider(
    /**
     * 将 PSI 元素映射到当前上下文模块的 provider。
     */
    private val moduleProvider: LLModuleProvider,

    /**
     * 将模块映射到可解析 session 的 provider。
     */
    private val sessionProvider: LLSessionProvider
) : LLDiagnosticProvider {
    /**
     * 收集 [file] 的文件级诊断。
     */
    override fun collectDiagnostics(file: CjFile, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        val module = moduleProvider.getModule(file)
        val moduleComponents = sessionProvider.getResolvableSession(module).moduleComponents
        return moduleComponents.diagnosticsCollector.collectDiagnosticsForFile(file, filter)
    }

    /**
     * 获取 [element] 上直接挂载的诊断。
     */
    override fun getDiagnostics(element: CjElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        val module = moduleProvider.getModule(element)
        val moduleComponents = sessionProvider.getResolvableSession(module).moduleComponents
        return moduleComponents.diagnosticsCollector.getDiagnosticsFor(element, filter)
    }
}
