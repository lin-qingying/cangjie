package org.cangjie.analysis.api.platform

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * 平台设置（对齐 Kotlin 的 KotlinPlatformSettings）。
 *
 * 允许平台控制 Analysis API 引擎的行为。
 */
interface CaPlatformSettings {
    /** 是否允许库模块作为分析的 use-site 模块 */
    val allowUseSiteLibraryModuleAnalysis: Boolean
        get() = true

    companion object {
        fun getInstance(project: Project): CaPlatformSettings = project.service()
    }
}
