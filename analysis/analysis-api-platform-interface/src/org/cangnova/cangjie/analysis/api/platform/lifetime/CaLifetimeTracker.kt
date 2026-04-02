package org.cangnova.cangjie.analysis.api.platform.lifetime

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken

/**
 * Analysis API 生命周期跟踪器。
 *
 * 它负责记录当前线程上正在生效的分析令牌，并为令牌实现提供可访问性校验依据。
 */
interface CaLifetimeTracker {
    /**
     * 当前活跃分析对应的生命周期令牌；如果当前线程不在分析块中则为 `null`。
     */
    val currentToken: CaLifetimeToken?

    companion object {
        fun getInstance(project: Project): CaLifetimeTracker = project.service()
    }
}
