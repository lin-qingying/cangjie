package org.cangnova.cangjie.analysis.api.impl.base.permissions

import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionOptions

/**
 * Analysis API 权限选项的基础实现。
 *
 * 对齐 Kotlin Analysis API 的默认约束：
 * - 默认不允许在 EDT 中执行分析
 * - 默认不允许在写动作中执行分析
 *
 * 更具体的平台可以通过覆盖服务实现来调整策略。
 */
internal class CaBaseAnalysisPermissionOptions : CaAnalysisPermissionOptions {
    override val defaultIsAnalysisAllowedOnEdt: Boolean
        get() = false

    override val defaultIsAnalysisAllowedInWriteAction: Boolean
        get() = false
}
