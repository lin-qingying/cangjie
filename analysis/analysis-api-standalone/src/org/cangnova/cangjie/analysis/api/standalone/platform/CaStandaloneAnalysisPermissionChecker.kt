package org.cangnova.cangjie.analysis.api.standalone.platform

import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker

/**
 * Standalone 平台的分析权限检查器。
 *
 * Standalone/测试环境没有 IDE 前台线程与写动作语义，若继续复用 IDE 默认策略，
 * Analysis API 会在 headless 场景下被错误地整体禁用。这里仅保留显式禁止分析的能力，
 * 由宿主在需要时通过 [CaAnalysisPermissionRegistry] 主动收紧权限。
 */
class CaStandaloneAnalysisPermissionChecker : CaAnalysisPermissionChecker {
    private val permissionRegistry by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaAnalysisPermissionRegistry.getInstance()
    }

    override fun isAnalysisAllowed(): Boolean {
        return permissionRegistry.explicitAnalysisRestriction == null
    }

    override fun getRejectionReason(): String {
        val restriction = permissionRegistry.explicitAnalysisRestriction
            ?: error("Cannot get a rejection reason when analysis is allowed.")
        return "Resolve is explicitly forbidden in the current action: ${restriction.description}."
    }
}
