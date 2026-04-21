package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getDiagnostics
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * CFIR 会话内的诊断与调用查询服务。
 *
 * 该层统一承载 low-level 查询以及 low-level -> public 的稳定映射。
 */
internal class CaCfirSessionDiagnosticQueryService(
    private val analysisSession: CaCfirSession,
    private val resolutionFacade: LLResolutionFacade,
    private val cacheStore: CaCfirSessionCacheStore,
) {
    fun queryDiagnostics(
        element: PsiElement,
        filter: DiagnosticCheckerFilter,
    ): List<CjPsiDiagnostic> = cacheStore.getOrCreateDiagnostics(element, filter) {
        (element as? CjElement)?.getDiagnostics(resolutionFacade, filter)?.toList().orEmpty()
    }

    fun queryFileDiagnostics(
        file: CjFile,
        filter: DiagnosticCheckerFilter,
    ): Collection<CjPsiDiagnostic> = cacheStore.getOrCreateFileDiagnostics(file, filter) {
        file.collectDiagnosticsForFile(resolutionFacade, filter)
    }
}
