package org.cangnova.cangjie.analysis.api.platform

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Analysis API 的平台设置。
 *
 * 该接口承载 IDE、Standalone、LSP 三类宿主对分析引擎行为的统一开关，
 * 避免把平台策略直接写死在 Analysis API 实现层。
 */
interface CaPlatformSettings {
    /**
     * 是否允许把库模块作为 use-site 模块直接进入分析。
     *
     * IDE 与 LSP 往往会更严格，Standalone 则可以根据调用方需要放宽。
     */
    val allowUseSiteLibraryModuleAnalysis: Boolean
        get() = true

    companion object {
        fun getInstance(project: Project): CaPlatformSettings = project.service()
    }
}
