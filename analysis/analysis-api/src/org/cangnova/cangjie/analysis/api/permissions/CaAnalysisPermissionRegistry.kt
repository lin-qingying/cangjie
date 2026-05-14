package org.cangnova.cangjie.analysis.api.permissions

import com.intellij.openapi.application.ApplicationManager

/**
 * 分析权限注册表（对齐 Kotlin 的 KaAnalysisPermissionRegistry）。
 *
 * 应用级服务，存储 [forbidAnalysis]、[allowAnalysisOnEdt]、[allowAnalysisFromWriteAction] 等权限函数的设置。
 */
interface CaAnalysisPermissionRegistry {
    /**
     * 显式禁止分析的限制条件。
     *
     * 由 [forbidAnalysis] 写入,违规分析时把 [description] 作为定位线索抛出。
     */
    class CaExplicitAnalysisRestriction(val description: String)

    /** 当前生效的显式禁止限制;无禁止时为 `null`。 */
    var explicitAnalysisRestriction: CaExplicitAnalysisRestriction?

    /** 是否允许在 EDT 上执行 analyze 块。 */
    var isAnalysisAllowedOnEdt: Boolean

    /** 是否允许在 write action 中执行 analyze 块。 */
    var isAnalysisAllowedInWriteAction: Boolean

    companion object {
        /** 获取 application 范围的注册表单例。 */
        fun getInstance(): CaAnalysisPermissionRegistry =
            ApplicationManager.getApplication().getService(CaAnalysisPermissionRegistry::class.java)
    }
}
