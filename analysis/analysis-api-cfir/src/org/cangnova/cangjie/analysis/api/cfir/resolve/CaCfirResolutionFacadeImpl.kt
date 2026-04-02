package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.psi.CjFile

/**
 * CFIR 解析外观实现。
 *
 * 该实现把底层 `CfirSession`、模块数据与诊断收集结果封装在一起，
 * 作为 Analysis API 与 CFIR 前端之间的低层桥接对象。
 */
class CaCfirResolutionFacadeImpl(
    override val useSiteModule: CaModule,
    override val useSiteFirSession: CfirSession,
    internal val moduleData: CfirModuleData,
    private val diagnosticCollector: CfirDiagnosticCollector,
) : CaCfirResolutionFacade {
    override fun getDiagnostics(element: PsiElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        return filteredPsiDiagnostics(filter)
            .filter { it.psiElement == element }
    }

    override fun collectDiagnosticsForFile(file: CjFile, filter: DiagnosticCheckerFilter): Collection<CjPsiDiagnostic> {
        return filteredPsiDiagnostics(filter)
            .filter { it.psiFile == file }
    }

    /**
     * 当前仓颉 CFIR 前端尚未把 common / extra / experimental 三类 checker 拆成独立诊断流，
     * 因此当前 facade 只能保证：
     * - default/common 过滤返回真实诊断；
     * - 仅 extra / 仅 experimental 过滤返回空集合；
     * - mixed 过滤退化为 common 结果。
     *
     * 这是对现有编译器能力的准确建模，而不是兜底行为。
     */
    private fun filteredPsiDiagnostics(filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        if (!filter.includesDefaultCheckers) {
            return emptyList()
        }

        return diagnosticCollector.rawDiagnostics
            .filterIsInstance<CjPsiDiagnostic>()
    }
}
