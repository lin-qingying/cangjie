package org.cangjie.analysis.api.session

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.cangjie.analysis.api.CaModule
import org.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.psi.CjElement

/**
 * 分析会话提供器（对齐 Kotlin 的 KaSessionProvider）。
 *
 * 负责创建和管理 [CaSession] 的生命周期。
 * 使用三阶段钩子：beforeEnteringAnalysis → action → afterLeavingAnalysis。
 */
abstract class CaSessionProvider(val project: Project) : Disposable {

    /** 获取分析会话 */
    abstract fun getAnalysisSession(useSiteElement: CjElement): CaSession
    abstract fun getAnalysisSession(useSiteModule: CaModule): CaSession

    /**
     * 在 [CaSession] 上下文中执行分析操作。
     *
     * inline 确保每次调用展开到字节码，避免 lambda 分配开销。
     * synchronized 块防止非本地暂停调用。
     */
    inline fun <R> analyze(
        useSiteElement: CjElement,
        action: CaSession.() -> R,
    ): R {
        val analysisSession = getAnalysisSession(useSiteElement)
        beforeEnteringAnalysis(analysisSession, useSiteElement)
        return try {
            val lock = Any()
            synchronized(lock) {
                analysisSession.action()
            }
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
            val lock = Any()
            synchronized(lock) {
                analysisSession.action()
            }
        } catch (throwable: Throwable) {
            handleAnalysisException(throwable, analysisSession, useSiteModule)
        } finally {
            afterLeavingAnalysis(analysisSession, useSiteModule)
        }
    }

    /** 进入分析前的钩子（权限检查、生命周期追踪等） */
    abstract fun beforeEnteringAnalysis(session: CaSession, useSiteElement: CjElement)
    abstract fun beforeEnteringAnalysis(session: CaSession, useSiteModule: CaModule)

    /** 异常处理（实现必须抛出异常） */
    abstract fun handleAnalysisException(throwable: Throwable, session: CaSession, useSiteElement: CjElement): Nothing
    abstract fun handleAnalysisException(throwable: Throwable, session: CaSession, useSiteModule: CaModule): Nothing

    /** 离开分析后的钩子（资源清理） */
    abstract fun afterLeavingAnalysis(session: CaSession, useSiteElement: CjElement)
    abstract fun afterLeavingAnalysis(session: CaSession, useSiteModule: CaModule)

    /** 清除缓存 */
    abstract fun clearCaches()

    companion object {
        fun getInstance(project: Project): CaSessionProvider =
            project.getService(CaSessionProvider::class.java)
    }
}
