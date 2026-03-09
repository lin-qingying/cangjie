package org.cangjie.analysis.api.platform.permissions

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * 分析权限检查器（对齐 Kotlin 的 KaAnalysisPermissionChecker）。
 *
 * 检查当前是否允许进行代码分析。以下情况可能禁止分析：
 * - 在 EDT 上调用分析（除非显式允许）
 * - 在写操作中调用分析（除非显式允许）
 * - 通过 forbidAnalysis 显式禁止
 */
interface CaAnalysisPermissionChecker {
    fun isAnalysisAllowed(): Boolean

    fun getRejectionReason(): String

    companion object {
        fun getInstance(project: Project): CaAnalysisPermissionChecker = project.service()
    }
}
