package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticProvider
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.impl.base.components.withPsiValidityAssertion
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getDiagnostics
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.plus
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * 诊断组件。
 */
internal class CaCfirDiagnosticProvider(
    /**
     * 延迟取得当前 CFIR Analysis session，诊断查询通过该 session 的 resolution facade 执行。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaDiagnosticProvider, CaCfirSessionComponent {

    @CaExperimentalApi
    /**
     * 收集指定 PSI 元素关联的诊断。
     */
    override fun CjElement.diagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>> = withPsiValidityAssertion {
        // 反编译 `.cjo` 视图不是源码：对它跑源码诊断会走 PSI→raw CFIR 构建，
        // 而反编译 PSI 会随 provider 重建失效，最终以 invalid element 异常中断诊断。
        if (containingCjFile?.isCompiled == true) return@withPsiValidityAssertion emptyList()
        getDiagnostics(resolutionFacade, filter.asLLFilter()).map { it.asCaDiagnostic() }
    }

    /**
     * 收集整个文件在指定诊断过滤器下产生的诊断。
     */
    override fun CjFile.collectDiagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>> =
        withPsiValidityAssertion {
            // 库/内建的 `.cjo` 反编译视图没有源码语义，诊断必须为空。
            if (isCompiled) return@withPsiValidityAssertion emptyList()
            collectDiagnosticsForFile(resolutionFacade, filter.asLLFilter()).map { it.asCaDiagnostic() }
        }

    /**
     * 将公开诊断过滤器映射为 low-level CFIR 诊断过滤器。
     */
    private fun CaDiagnosticCheckerFilter.asLLFilter(): DiagnosticCheckerFilter = when (this) {
        CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS -> DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS
        CaDiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS
        CaDiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS
        CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS ->
            DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS + DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS
    }
}
