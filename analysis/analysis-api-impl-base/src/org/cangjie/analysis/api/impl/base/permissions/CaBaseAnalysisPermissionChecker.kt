package org.cangjie.analysis.api.impl.base.permissions

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import org.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry
import org.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker
import org.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionOptions

/**
 * 分析权限检查器的基础实现（对齐 Kotlin 的 KaBaseAnalysisPermissionChecker）。
 *
 * 检查 EDT、写操作、显式禁止等条件。
 */
internal class CaBaseAnalysisPermissionChecker : CaAnalysisPermissionChecker {

    private val permissionRegistry by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaAnalysisPermissionRegistry.getInstance()
    }

    private val permissionOptions by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaAnalysisPermissionOptions.getInstance()
    }

    override fun isAnalysisAllowed(): Boolean {
        val application = ApplicationManager.getApplication()

        if (isProhibitedEdtAnalysis(application)) return false
        if (isProhibitedWriteActionAnalysis(application)) return false
        if (permissionRegistry.explicitAnalysisRestriction != null) return false

        return true
    }

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

    private fun isProhibitedEdtAnalysis(application: Application): Boolean =
        application.isDispatchThread &&
                !permissionOptions.defaultIsAnalysisAllowedOnEdt &&
                !permissionRegistry.isAnalysisAllowedOnEdt

    private fun isProhibitedWriteActionAnalysis(application: Application): Boolean =
        application.isWriteAccessAllowed &&
                !permissionOptions.defaultIsAnalysisAllowedInWriteAction &&
                !permissionRegistry.isAnalysisAllowedInWriteAction
}
