

package org.cangnova.cangjie.analysis.low.level.api.cfir.state

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLDiagnosticsStatistics
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import kotlin.time.TimeSource

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
    private val sessionProvider: LLSessionProvider,

    /**
     * 诊断收集统计域；统计未启用时为 `null`，此时两条路径都不取单调时钟。
     */
    private val diagnosticsStatistics: LLDiagnosticsStatistics?,
) : LLDiagnosticProvider {
    /**
     * 收集 [file] 的文件级诊断。
     *
     * 这是 IDE 用户实际等待的那一次调用。耗时从进入本方法起算，包含 session 取得、文件结构
     * 首次构建与全部 checker 遍历，因此它是判断"检查这个文件慢不慢"的唯一正确指标；
     * `structureBuild` 与 `checkerPass` 是它的分解，不是平级指标。
     */
    override fun collectDiagnostics(file: CjFile, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        val statistics = diagnosticsStatistics
        if (statistics == null) return collectDiagnosticsWithoutStatistics(file, filter)

        statistics.onFileCollectionStarted()
        val startedAt = TimeSource.Monotonic.markNow()
        var diagnostics: List<CjPsiDiagnostic>? = null
        try {
            diagnostics = collectDiagnosticsWithoutStatistics(file, filter)
            return diagnostics
        } finally {
            // 失败同样记录：失败的收集也是用户等掉的耗时，与其余接缝的失败口径一致。
            statistics.onFileCollectionFinished(startedAt.elapsedNow().inWholeNanoseconds, diagnostics?.size ?: 0)
        }
    }

    /**
     * 文件级诊断收集的实际执行体，由计时包装与零开销路径共用。
     */
    private fun collectDiagnosticsWithoutStatistics(file: CjFile, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        val module = moduleProvider.getModule(file)
        val moduleComponents = sessionProvider.getResolvableSession(module).moduleComponents
        return moduleComponents.diagnosticsCollector.collectDiagnosticsForFile(file, filter)
    }

    /**
     * 获取 [element] 上直接挂载的诊断。
     *
     * 元素级查询由补全、导航等高频路径触发，单次很轻；单独计数是为了在文件级耗时异常时
     * 判断是不是高频元素查询堆积，而不是 checker 本身变慢。
     */
    override fun getDiagnostics(element: CjElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        val statistics = diagnosticsStatistics
        val module = moduleProvider.getModule(element)
        val moduleComponents = sessionProvider.getResolvableSession(module).moduleComponents
        if (statistics == null) return moduleComponents.diagnosticsCollector.getDiagnosticsFor(element, filter)

        statistics.onElementCollectionStarted()
        val startedAt = TimeSource.Monotonic.markNow()
        var diagnostics: List<CjPsiDiagnostic>? = null
        try {
            diagnostics = moduleComponents.diagnosticsCollector.getDiagnosticsFor(element, filter)
            return diagnostics
        } finally {
            // 失败同样记录：失败的收集也是用户等掉的耗时，与其余接缝的失败口径一致。
            statistics.onElementCollectionFinished(startedAt.elapsedNow().inWholeNanoseconds, diagnostics?.size ?: 0)
        }
    }
}
