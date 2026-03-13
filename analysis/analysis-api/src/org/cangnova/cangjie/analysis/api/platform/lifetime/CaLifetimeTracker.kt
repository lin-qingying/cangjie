package org.cangnova.cangjie.analysis.api.platform.lifetime

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken

/**
 * 生命周期追踪器（对齐 Kotlin 的 KaLifetimeTracker）。
 *
 * 追踪当前活跃的 [CaLifetimeToken]，用于自定义令牌实现中检查令牌是否在作用域内。
 */
interface CaLifetimeTracker {
    /** 当前活跃分析的生命周期令牌，如果没有正在进行的分析则为 null */
    val currentToken: CaLifetimeToken?

    companion object {
        fun getInstance(project: Project): CaLifetimeTracker = project.service()
    }
}
