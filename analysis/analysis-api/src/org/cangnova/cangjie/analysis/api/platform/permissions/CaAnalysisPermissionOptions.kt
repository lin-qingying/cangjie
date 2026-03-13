package org.cangjie.analysis.api.platform.permissions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service

/**
 * 分析权限选项（对齐 Kotlin 的 KotlinAnalysisPermissionOptions）。
 *
 * 允许平台选择是否默认允许在 EDT 和写操作中进行分析。
 */
interface CaAnalysisPermissionOptions {
    /** 默认是否允许在 EDT 上分析 */
    val defaultIsAnalysisAllowedOnEdt: Boolean

    /** 默认是否允许在写操作中分析 */
    val defaultIsAnalysisAllowedInWriteAction: Boolean

    companion object {
        fun getInstance(): CaAnalysisPermissionOptions = ApplicationManager.getApplication().service()
    }
}
