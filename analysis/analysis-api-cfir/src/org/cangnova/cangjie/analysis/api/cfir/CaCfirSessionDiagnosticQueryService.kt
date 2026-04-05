package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallInfoSnapshot
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirResolutionFacade
import org.cangnova.cangjie.analysis.api.cfir.resolve.DiagnosticCheckerFilter
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.psi.CjFile

/**
 * CFIR 会话内的诊断与调用快照查询服务。
 *
 * 这一层专门承载“与诊断管线和调用解析快照有关”的 low-level 查询，
 * 让会话编排层不再混杂作用域、类型和符号导航逻辑。
 */
internal class CaCfirSessionDiagnosticQueryService(
    private val resolutionFacade: CaCfirResolutionFacade,
    private val cacheStore: CaCfirSessionCacheStore,
) {
    fun queryCallInfo(element: PsiElement): CaCfirCallInfoSnapshot? =
        cacheStore.getOrCreateCallInfo(element) {
            resolutionFacade.getCallInfo(element)
        }

    fun queryDiagnostics(
        element: PsiElement,
        filter: DiagnosticCheckerFilter,
    ): List<CjPsiDiagnostic> = cacheStore.getOrCreateDiagnostics(element, filter) {
        resolutionFacade.getDiagnostics(element, filter)
    }

    fun queryFileDiagnostics(
        file: CjFile,
        filter: DiagnosticCheckerFilter,
    ): Collection<CjPsiDiagnostic> = cacheStore.getOrCreateFileDiagnostics(file, filter) {
        resolutionFacade.collectDiagnosticsForFile(file, filter)
    }
}
