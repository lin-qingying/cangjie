package org.cangnova.cangjie.analysis.api.impl.base.permissions

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionOptions

/**
 * 分析权限检查器的基础实现（对齐 Kotlin 的 KaBaseAnalysisPermissionChecker）。
 *
 * 检查 EDT、写操作、显式禁止等条件。
 */
internal class CaBaseAnalysisPermissionChecker : CaAnalysisPermissionChecker {

    /**
     * 当前线程可变分析权限注册表。
     */
    private val permissionRegistry by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaAnalysisPermissionRegistry.getInstance()
    }

    /**
     * 平台默认分析权限选项。
     */
    private val permissionOptions by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaAnalysisPermissionOptions.getInstance()
    }

    /**
     * 判断当前线程和 application 状态是否允许进入分析。
     */
    override fun isAnalysisAllowed(): Boolean {
        val application = ApplicationManager.getApplication()

        if (isProhibitedEdtAnalysis(application)) return false
        if (isProhibitedWriteActionAnalysis(application)) return false
        if (permissionRegistry.explicitAnalysisRestriction != null) return false

        return true
    }

    /**
     * 返回当前禁止分析的具体原因。
     */
    override fun getRejectionReason(): String {
        val application = ApplicationManager.getApplication()

        if (isProhibitedEdtAnalysis(application)) {
            return "Called in the EDT thread."
        }

        if (isProhibitedWriteActionAnalysis(application)) {
            return "Called from a write action."
        }

        permissionRegistry.explicitAnalysisRestriction?.let { restriction ->
            return "Resolve is explicitly forbidden in the current action: ${restriction.description}."
        }

        error("Cannot get a rejection reason when analysis is allowed.")
    }

    /**
     * 判断当前 EDT 分析是否被禁止。
     */
    private fun isProhibitedEdtAnalysis(application: Application): Boolean =
        application.isDispatchThread &&
                !permissionOptions.defaultIsAnalysisAllowedOnEdt &&
                !permissionRegistry.isAnalysisAllowedOnEdt

    /**
     * 判断当前写动作内分析是否被禁止。
     */
    private fun isProhibitedWriteActionAnalysis(application: Application): Boolean =
        application.isWriteAccessAllowed &&
                !permissionOptions.defaultIsAnalysisAllowedInWriteAction &&
                !permissionRegistry.isAnalysisAllowedInWriteAction
}
