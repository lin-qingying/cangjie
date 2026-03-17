package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent

/**
 * 会话组件基类，为子类提供对分析会话与生命周期令牌的统一访问。
 */
abstract class CaBaseSessionComponent<T : CaSession> : CaSessionComponent {
    /** 返回当前组件绑定的分析会话。 */
    abstract val analysisSessionProvider: () -> T

    /** 组件所属的分析会话实例。 */
    val analysisSession: T
        get() = analysisSessionProvider()

    /** 复用分析会话的生命周期令牌。 */
    final override val token: CaLifetimeToken
        get() = analysisSession.token
}
