package org.cangnova.cangjie.analysis.api.standalone.base.permissions

import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionOptions

/**
 * Standalone 宿主的 Analysis 权限选项。
 *
 * 对齐 Kotlin `KotlinStandaloneAnalysisPermissionOptions`：
 * standalone/headless 宿主默认允许在 EDT 与 write action 中进入分析，
 * 仅由显式 restriction 收紧。
 */
class CaStandaloneAnalysisPermissionOptions : CaAnalysisPermissionOptions {
    override val defaultIsAnalysisAllowedOnEdt: Boolean
        get() = true

    override val defaultIsAnalysisAllowedInWriteAction: Boolean
        get() = true
}
