package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.psi.CjFile

/**
 * 最小可用的 CFIR 解析外观实现。
 *
 * 持有 session 与诊断收集器，供测试和最小解析入口使用。
 */
class CaCfirResolutionFacadeImpl(
    override val useSiteModule: CaModule,
    override val useSiteFirSession: CfirSession,
    val diagnosticCollector: CfirDiagnosticCollector,
) : CaCfirResolutionFacade {

    override fun getDiagnostics(element: PsiElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        // 从收集器中过滤出属于指定元素的 PSI 诊断
        return diagnosticCollector.rawDiagnostics
            .filterIsInstance<CjPsiDiagnostic>()
            .filter { it.psiElement == element }
    }

    override fun collectDiagnosticsForFile(file: CjFile, filter: DiagnosticCheckerFilter): Collection<CjPsiDiagnostic> {
        // 从收集器中过滤出属于指定文件的 PSI 诊断
        return diagnosticCollector.rawDiagnostics
            .filterIsInstance<CjPsiDiagnostic>()
            .filter { it.psiFile == file }
    }
}
