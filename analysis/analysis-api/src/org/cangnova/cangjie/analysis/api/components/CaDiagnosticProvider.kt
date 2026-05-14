package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponentImplementationDetail
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * 诊断信息查询协议。
 *
 * 设计要点/职责:
 * - 暴露对元素与文件粒度的诊断收集入口,过滤维度统一由 [CaDiagnosticCheckerFilter] 控制。
 * - 元素级 API 不保证子元素诊断完整,文件级 API 才是完整快照;调用方应优先使用 [CjFile.collectDiagnostics]。
 *
 * 对齐 Kotlin Analysis API 的 `KaDiagnosticProvider`。
 */
@CaSessionComponentImplementationDetail
@SubclassOptInRequired(CaSessionComponentImplementationDetail::class)
interface CaDiagnosticProvider : CaLifetimeOwner {
    /**
     * 收集元素本身关联的诊断。
     *
     * 注意:可能不包含子元素或上层 checker 的诊断,
     * 因而面向完整结果时应使用 [CjFile.collectDiagnostics]。
     */
    @CaExperimentalApi

    fun CjElement.diagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>>

    /**
     * 收集整个文件下所有诊断,作为元素级 API 的完整对应物。
     */
    fun CjFile.collectDiagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>>
}
