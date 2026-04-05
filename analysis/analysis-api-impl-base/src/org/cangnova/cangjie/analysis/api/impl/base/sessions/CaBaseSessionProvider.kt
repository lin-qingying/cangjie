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

    /**
     * 基础实现把批量元素分析收敛为“按同一 session 分组后只进入一次分析域”。
     *
     * 这保证了：
     * 1. 同一 use-site session 共享统一 lifetime 与缓存边界；
     * 2. permission / restricted-analysis / write-action 检查只在批次边界执行一次；
     * 3. 平台层无需各自手写 session 分组逻辑。
     */
    override fun <R> analyzeElements(
        useSiteElements: Collection<CjElement>,
        action: CaSession.(CjElement) -> R,
    ): List<R> {
        if (useSiteElements.isEmpty()) return emptyList()

        val groupedElements = LinkedHashMap<CaSession, MutableList<Pair<Int, CjElement>>>()
        useSiteElements.withIndex().forEach { indexedElement ->
            PsiUtilCore.ensureValid(indexedElement.value)
            val session = getAnalysisSession(indexedElement.value)
            groupedElements.getOrPut(session) { mutableListOf() }
                .add(indexedElement.index to indexedElement.value)
        }

        val results = arrayOfNulls<Any?>(useSiteElements.size)
        groupedElements.forEach { (session, entries) ->
            val representative = entries.first().second
            beforeEnteringAnalysis(session, representative)
            try {
                entries.forEach { (index, element) ->
                    results[index] = session.action(element)
                }
            } catch (throwable: Throwable) {
                handleAnalysisException(throwable, session, representative)
            } finally {
                afterLeavingAnalysis(session, representative)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return results.map { it as R }
    }

    /**
     * 基础实现把批量模块分析也收敛到 session 粒度。
     */
    override fun <R> analyzeModules(
        useSiteModules: Collection<CaModule>,
        action: CaSession.(CaModule) -> R,
    ): List<R> {
        if (useSiteModules.isEmpty()) return emptyList()

        val groupedModules = LinkedHashMap<CaSession, MutableList<Pair<Int, CaModule>>>()
        useSiteModules.withIndex().forEach { indexedModule ->
            val session = getAnalysisSession(indexedModule.value)
            groupedModules.getOrPut(session) { mutableListOf() }
                .add(indexedModule.index to indexedModule.value)
        }

        val results = arrayOfNulls<Any?>(useSiteModules.size)
        groupedModules.forEach { (session, entries) ->
            val representative = entries.first().second
            beforeEnteringAnalysis(session, representative)
            try {
                entries.forEach { (index, module) ->
                    results[index] = session.action(module)
                }
            } catch (throwable: Throwable) {
                handleAnalysisException(throwable, session, representative)
            } finally {
                afterLeavingAnalysis(session, representative)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return results.map { it as R }
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
