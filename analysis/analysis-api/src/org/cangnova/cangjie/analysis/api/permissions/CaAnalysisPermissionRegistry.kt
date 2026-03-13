package org.cangjie.analysis.api.permissions

import com.intellij.openapi.application.ApplicationManager

/**
 * 分析权限注册表（对齐 Kotlin 的 KaAnalysisPermissionRegistry）。
 *
 * 应用级服务，存储 [forbidAnalysis]、[allowAnalysisOnEdt]、[allowAnalysisFromWriteAction] 等权限函数的设置。
 */
interface CaAnalysisPermissionRegistry {
    class CaExplicitAnalysisRestriction(val description: String)

    var explicitAnalysisRestriction: CaExplicitAnalysisRestriction?

    var isAnalysisAllowedOnEdt: Boolean

    var isAnalysisAllowedInWriteAction: Boolean

    companion object {
        fun getInstance(): CaAnalysisPermissionRegistry =
            ApplicationManager.getApplication().getService(CaAnalysisPermissionRegistry::class.java)
    }
}
