package org.cangnova.cangjie.cfir.resolve.match.exhaustive

import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * match 穷尽性分析公共上下文。
 *
 * 该上下文只暴露穷尽性算法实际需要的能力：会话 [session]。
 * 通过此抽象消除对 checkers `CheckerContext` 的硬依赖，使 BODY_RESOLVE 与 CHECKERS 可共享同一实现。
 */
interface MatchExhaustivenessContext {
    val session: CfirSession

    companion object {
        fun fromSession(session: CfirSession): MatchExhaustivenessContext {
            return SessionMatchExhaustivenessContext(session)
        }
    }
}

data class SessionMatchExhaustivenessContext(
    override val session: CfirSession,
) : MatchExhaustivenessContext
