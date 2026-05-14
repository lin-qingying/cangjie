package org.cangnova.cangjie.analysis.api.session

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.psi.CjElement

/**
 * 分析会话提供器。
 *
 * 对齐 Kotlin `KaSessionProvider`，负责创建并管理 [CaSession] 的生命周期。
 * `analyze()` 本身不负责额外同步；真正的并发约束应由 token、生命周期追踪器和平台层服务承担。
 */
abstract class CaSessionProvider(val project: Project) : Disposable {
    /** 为指定的源码元素构造或复用一个 [CaSession]。 */
    abstract fun getAnalysisSession(useSiteElement: CjElement): CaSession

    /** 为指定的 [CaModule] 构造或复用一个 [CaSession]。 */
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

    /**
     * 在 [CaSession] 上下文中按模块视角执行分析动作。
     *
     * 与按元素入口配套,适用于"目标模块明确、入口元素无关"的批量场景。
     */
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

    /** 在进入 analyze 块前调用,平台层可在此挂入计时、断点、读锁请求等。 */
    abstract fun beforeEnteringAnalysis(session: CaSession, useSiteElement: CjElement)

    /** 按模块入口的进入钩子,与按元素的版本语义对齐。 */
    abstract fun beforeEnteringAnalysis(session: CaSession, useSiteModule: CaModule)

    /**
     * 在 [action] 抛出异常时统一处理。
     *
     * 平台实现必须重抛或包装异常,不允许吞掉(返回 `Nothing`)。
     */
    abstract fun handleAnalysisException(throwable: Throwable, session: CaSession, useSiteElement: CjElement): Nothing

    /** 按模块入口的异常处理器,与按元素的版本语义对齐。 */
    abstract fun handleAnalysisException(throwable: Throwable, session: CaSession, useSiteModule: CaModule): Nothing

    /** 在 analyze 块正常或异常退出后调用,平台层可在此回收 token、释放读锁等。 */
    abstract fun afterLeavingAnalysis(session: CaSession, useSiteElement: CjElement)

    /** 按模块入口的离开钩子。 */
    abstract fun afterLeavingAnalysis(session: CaSession, useSiteModule: CaModule)

    /** 清空所有缓存的 session 与派生数据,通常在文档大量变更或工程重载时调用。 */
    abstract fun clearCaches()

    companion object {
        /** 获取当前 [Project] 上的 session provider 服务。 */
        fun getInstance(project: Project): CaSessionProvider =
            project.getService(CaSessionProvider::class.java)
    }
}
