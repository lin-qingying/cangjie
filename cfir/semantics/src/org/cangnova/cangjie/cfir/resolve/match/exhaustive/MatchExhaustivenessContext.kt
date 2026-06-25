package org.cangnova.cangjie.cfir.resolve.match.exhaustive

import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * match 穷尽性分析公共上下文。
 *
 * 该上下文只暴露穷尽性算法实际需要的能力：会话 [session]。
 * 通过此抽象消除对 checkers `CheckerContext` 的硬依赖，使 BODY_RESOLVE 与 CHECKERS 可共享同一实现。
 */
interface MatchExhaustivenessContext {
    /** 穷尽性分析需要访问的 CFIR session。 */
    val session: CfirSession

    /**
     * 上下文构造工具。
     */
    companion object {
        /**
         * 使用 session 创建最小穷尽性分析上下文。
         */
        fun fromSession(session: CfirSession): MatchExhaustivenessContext {
            return SessionMatchExhaustivenessContext(session)
        }
    }
}

/**
 * 仅由 session 构成的穷尽性分析上下文。
 *
 * @property session 当前分析使用的 CFIR session。
 */
data class SessionMatchExhaustivenessContext(
    /**
     * 当前分析使用的 CFIR session。
     */
    override val session: CfirSession,
) : MatchExhaustivenessContext
