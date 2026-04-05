package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent

/**
 * 会话组件基类。
 *
 * 该基类为子类统一提供分析会话与生命周期 token 的访问入口，
 * 避免每个组件重复实现相同的会话绑定代码。
 */
abstract class CaBaseSessionComponent<T : CaSession> : CaSessionComponent {
    /**
     * 返回当前组件绑定的分析会话提供器。
     */
    abstract val analysisSessionProvider: () -> T

    /**
     * 组件所属的分析会话实例。
     */
    val analysisSession: T
        get() = analysisSessionProvider()

    /**
     * 复用分析会话的生命周期 token。
     */
    final override val token: CaLifetimeToken
        get() = analysisSession.token
}
