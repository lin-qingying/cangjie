package org.cangnova.cangjie.analysis.api.session

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.psi.CjElement

/**
 * 分析会话提供器。
 *
 * 对齐 Kotlin `KaSessionProvider`，负责创建并管理 [CaSession] 的生命周期。
 * `analyze()` 本身不负责额外同步；真正的并发约束应由 token、生命周期追踪器和平台层服务承担。
 */
abstract class CaSessionProvider(val project: Project) : Disposable {
    abstract fun getAnalysisSession(useSiteElement: CjElement): CaSession

    abstract fun getAnalysisSession(useSiteModule: CaModule): CaSession

    /**
     * 在 [CaSession] 上下文中执行分析动作。
     */
    inline fun <R> analyze(
        useSiteElement: CjElement,
        action: CaSession.() -> R,
    ): R {
        val analysisSession = getAnalysisSession(useSiteElement)
        beforeEnteringAnalysis(analysisSession, useSiteElement)
        return try {
            analysisSession.action()
        } catch (throwable: Throwable) {
            handleAnalysisException(throwable, analysisSession, useSiteElement)
        } finally {
            afterLeavingAnalysis(analysisSession, useSiteElement)
        }
    }

    inline fun <R> analyze(
        useSiteModule: CaModule,
        action: CaSession.() -> R,
    ): R {
        val analysisSession = getAnalysisSession(useSiteModule)
        beforeEnteringAnalysis(analysisSession, useSiteModule)
        return try {
            analysisSession.action()
        } catch (throwable: Throwable) {
            handleAnalysisException(throwable, analysisSession, useSiteModule)
        } finally {
            afterLeavingAnalysis(analysisSession, useSiteModule)
        }
    }

    /**
     * 批量按源码元素执行分析。
     *
     * 默认实现保持协议语义正确性：逐元素进入 `analyze`。
     * 具体平台可覆写为“按 session 分组后批量进入分析域”的高效实现。
     */
    open fun <R> analyzeElements(
        useSiteElements: Collection<CjElement>,
        action: CaSession.(CjElement) -> R,
    ): List<R> {
        return useSiteElements.map { element ->
            analyze(element) { action(element) }
        }
    }

    /**
     * 批量按 use-site 模块执行分析。
     *
     * 默认实现仍逐模块进入 `analyze`；具体平台可按 session 复用优化。
     */
    open fun <R> analyzeModules(
        useSiteModules: Collection<CaModule>,
        action: CaSession.(CaModule) -> R,
    ): List<R> {
        return useSiteModules.map { module ->
            analyze(module) { action(module) }
        }
    }

    abstract fun beforeEnteringAnalysis(session: CaSession, useSiteElement: CjElement)

    abstract fun beforeEnteringAnalysis(session: CaSession, useSiteModule: CaModule)

    abstract fun handleAnalysisException(throwable: Throwable, session: CaSession, useSiteElement: CjElement): Nothing

    abstract fun handleAnalysisException(throwable: Throwable, session: CaSession, useSiteModule: CaModule): Nothing

    abstract fun afterLeavingAnalysis(session: CaSession, useSiteElement: CjElement)

    abstract fun afterLeavingAnalysis(session: CaSession, useSiteModule: CaModule)

    abstract fun clearCaches()

    companion object {
        fun getInstance(project: Project): CaSessionProvider =
            project.getService(CaSessionProvider::class.java)
    }
}
