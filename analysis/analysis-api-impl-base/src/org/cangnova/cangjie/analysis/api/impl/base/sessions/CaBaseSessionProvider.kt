package org.cangnova.cangjie.analysis.api.impl.base.sessions

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiUtilCore
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.impl.base.lifetime.CaBaseLifetimeTracker
import org.cangnova.cangjie.analysis.api.impl.base.permissions.CaBaseWriteActionStartedChecker
import org.cangnova.cangjie.analysis.api.impl.base.restrictedAnalysis.CaBaseRestrictedAnalysisException
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.lifetime.CaLifetimeTokenFactory
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker
import org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis.CaRestrictedAnalysisService
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.psi.CjElement

/**
 * CaSessionProvider 的基础实现（对齐 Kotlin 的 KaBaseSessionProvider）。
 *
 * 提供三阶段钩子的完整逻辑：
 * - beforeEnteringAnalysis: PSI 有效性检查、权限检查、生命周期追踪、写操作检查
 * - handleAnalysisException: 受限分析异常包装
 * - afterLeavingAnalysis: 写操作检查清理、生命周期追踪清理
 *
 * 子类（如 CaCfirSessionProvider）继承此类，只需实现 session 创建和缓存策略。
 */
abstract class CaBaseSessionProvider(project: Project) : CaSessionProvider(project) {

    private val permissionChecker by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaAnalysisPermissionChecker.getInstance(project)
    }

    private val lifetimeTracker by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaBaseLifetimeTracker.getInstance(project)
    }

    private val restrictedAnalysisService by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaRestrictedAnalysisService.getInstance(project)
    }

    protected val tokenFactory by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaLifetimeTokenFactory.getInstance(project)
    }

    private val platformSettings by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaPlatformSettings.getInstance(project)
    }

    private val writeActionStartedChecker = CaBaseWriteActionStartedChecker(this)

    override fun beforeEnteringAnalysis(session: CaSession, useSiteElement: CjElement) {
        PsiUtilCore.ensureValid(useSiteElement)
        beforeEnteringAnalysis(session)
    }

    override fun beforeEnteringAnalysis(session: CaSession, useSiteModule: CaModule) {
        beforeEnteringAnalysis(session)
    }

    private fun beforeEnteringAnalysis(session: CaSession) {
        if (!permissionChecker.isAnalysisAllowed()) {
            throw ProhibitedAnalysisException("Analysis is not allowed: ${permissionChecker.getRejectionReason()}")
        }

        ProgressManager.checkCanceled()

        restrictedAnalysisService?.run {
            if (isAnalysisRestricted && !isRestrictedAnalysisAllowed) {
                rejectRestrictedAnalysis()
            }
        }

        lifetimeTracker.beforeEnteringAnalysis(session)
        writeActionStartedChecker.beforeEnteringAnalysis()
    }

    override fun handleAnalysisException(throwable: Throwable, session: CaSession, useSiteElement: CjElement): Nothing {
        handleAnalysisException(throwable)
    }

    override fun handleAnalysisException(throwable: Throwable, session: CaSession, useSiteModule: CaModule): Nothing {
        handleAnalysisException(throwable)
    }

    private fun handleAnalysisException(throwable: Throwable): Nothing {
        if (restrictedAnalysisService?.isAnalysisRestricted == true && throwable !is Error) {
            throw CaBaseRestrictedAnalysisException(cause = throwable)
        }

        throw throwable
    }

    override fun afterLeavingAnalysis(session: CaSession, useSiteElement: CjElement) {
        afterLeavingAnalysis(session)
    }

    override fun afterLeavingAnalysis(session: CaSession, useSiteModule: CaModule) {
        afterLeavingAnalysis(session)
    }

    private fun afterLeavingAnalysis(session: CaSession) {
        try {
            writeActionStartedChecker.afterLeavingAnalysis()
        } finally {
            lifetimeTracker.afterLeavingAnalysis(session)
        }
    }
}

private class ProhibitedAnalysisException(override val message: String) : IllegalStateException()
