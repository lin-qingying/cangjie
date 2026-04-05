package org.cangnova.cangjie.analysis.test.services

import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker

/**
 * IDE 测试宿主专用的 Analysis 权限检查器。
 *
 * 生产态 IDE 仍然通过默认权限检查器约束 EDT / write action 下的分析入口；
 * 测试宿主则显式放宽这两个入口约束，只保留“显式禁止分析”这一条硬限制，
 * 以便 generated tests 在 MockProject 的测试调度环境中稳定进入 Analysis API。
 *
 * 这样既不会退化生产实现的权限模型，也能把“测试环境允许分析”这件事
 * 明确收敛到测试服务注册层，而不是散落在用例代码中临时开关权限。
 */
class CaTestIdeAnalysisPermissionChecker : CaAnalysisPermissionChecker {
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
