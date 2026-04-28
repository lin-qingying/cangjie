package org.cangnova.cangjie.analysis.api.impl.base.components

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent

/**
 * `analysis-api-impl-base` 侧的通用 session component 基类。
 *
 * 这里不绑定任何具体后端，只统一承载：
 * 1. 当前组件关联的分析会话；
 * 2. 会话生命周期 token。
 */
@CaImplementationDetail
abstract class CaBaseSessionComponent<T : CaSession> : CaSessionComponent {
    abstract val analysisSessionProvider: () -> T

    val analysisSession: T
        get() = analysisSessionProvider()

    final override val token: CaLifetimeToken
        get() = analysisSession.token
}

/**
 * 在 session component context 中直接取得当前分析会话。
 */
@CaImplementationDetail
context(sessionComponent: CaBaseSessionComponent<T>)
val <T : CaSession> analysisSession: T
    get() = sessionComponent.analysisSession
