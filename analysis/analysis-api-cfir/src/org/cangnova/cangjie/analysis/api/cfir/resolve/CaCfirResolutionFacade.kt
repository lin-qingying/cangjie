package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.psi.CjFile

/**
 * CFIR 解析外观（对齐 Kotlin 的 LLResolutionFacade / LLFirResolutionFacade）。
 *
 * Analysis API 与 CFIR 编译器之间的桥梁。
 * 封装底层 CfirSession 和解析能力，避免 Analysis API 直接依赖编译器内部。
 */
interface CaCfirResolutionFacade {
    /** 用途模块 */
    val useSiteModule: CaModule

    /** 底层 CFIR session */
    val useSiteFirSession: CfirSession

    /** 获取指定元素的诊断（对齐 K2 的 LLResolutionFacade.getDiagnostics） */
    fun getDiagnostics(element: PsiElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic>

    /** 收集指定文件的所有诊断（对齐 K2 的 LLResolutionFacade.collectDiagnosticsForFile） */
    fun collectDiagnosticsForFile(file: CjFile, filter: DiagnosticCheckerFilter): Collection<CjPsiDiagnostic>
}
