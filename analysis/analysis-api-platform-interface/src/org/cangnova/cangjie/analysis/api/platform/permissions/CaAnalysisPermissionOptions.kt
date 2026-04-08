package org.cangnova.cangjie.analysis.api.platform.permissions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service

/**
 * Analysis API 权限默认选项。
 *
 * 该接口描述宿主平台的默认权限政策，供权限检查器与测试框架复用。
 */
interface CaAnalysisPermissionOptions {
    /**
     * 默认是否允许在 EDT 上进入分析。
     */
    val defaultIsAnalysisAllowedOnEdt: Boolean

    /**
     * 默认是否允许在写动作中进入分析。
     */
    val defaultIsAnalysisAllowedInWriteAction: Boolean

    companion object {
        fun getInstance(): CaAnalysisPermissionOptions = ApplicationManager.getApplication().service()
    }
}
