package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent

abstract class CaBaseSessionComponent<T : CaSession> : CaSessionComponent {
    abstract val analysisSessionProvider: () -> T

    val analysisSession: T
        get() = analysisSessionProvider()

    final override val token: CaLifetimeToken
        get() = analysisSession.token
}
