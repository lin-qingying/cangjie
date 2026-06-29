package org.cangnova.cangjie.analysis.api.platform.permissions

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Analysis API 权限检查器。
 *
 * 平台在这里统一约束“什么时候允许进入分析”，例如：
 * - 是否允许在 EDT 上分析。
 * - 是否允许在写动作中分析。
 * - 是否被显式禁止分析。
 */
interface CaAnalysisPermissionChecker {
    /**
     * 当前线程和平台状态是否允许进入 Analysis API。
     */
    fun isAnalysisAllowed(): Boolean

    /**
     * 分析被拒绝时返回可读原因。
     */
    fun getRejectionReason(): String

    companion object {
        /**
         * 获取项目级分析权限检查器服务。
         */
        fun getInstance(project: Project): CaAnalysisPermissionChecker = project.service()
    }
}
