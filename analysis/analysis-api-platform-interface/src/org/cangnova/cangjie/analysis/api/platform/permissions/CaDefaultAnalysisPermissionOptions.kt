package org.cangnova.cangjie.analysis.api.platform.permissions

import org.cangnova.cangjie.analysis.api.CaPlatformInterface

/**
 * IDE 宿主的默认 Analysis 权限选项。
 *
 * 对齐 Kotlin `KotlinDefaultAnalysisPermissionOptions`：
 * 默认禁止在 EDT 与 write action 中进入分析，
 * 由具体宿主通过显式 opt-in 或替换 service 放宽。
 */
@CaPlatformInterface
class CaDefaultAnalysisPermissionOptions : CaAnalysisPermissionOptions {
    /**
     * 默认不允许在 EDT 中执行分析。
     */
    override val defaultIsAnalysisAllowedOnEdt: Boolean
        get() = false

    /**
     * 默认不允许在 write action 中执行分析。
     */
    override val defaultIsAnalysisAllowedInWriteAction: Boolean
        get() = false
}
