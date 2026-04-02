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
