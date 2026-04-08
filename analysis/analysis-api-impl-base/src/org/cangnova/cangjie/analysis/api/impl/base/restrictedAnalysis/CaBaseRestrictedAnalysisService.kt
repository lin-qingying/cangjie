package org.cangnova.cangjie.analysis.api.impl.base.restrictedAnalysis

import org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis.CaRestrictedAnalysisService

/**
 * 受限分析平台服务的基础实现。
 *
 * Kotlin Analysis API 在平台层始终显式建模“当前是否处于 restricted analysis”。
 * 仓颉的默认 IDE/生产实现先提供一个稳定、可覆盖的基类：
 * 1. 默认不处于受限分析模式；
 * 2. 默认允许完整分析；
 * 3. 真正进入受限模式时，由具体平台覆写状态与拒绝行为。
 *
 * 这样 session provider 不需要猜测“没有服务是否就等于不受限”，
 * 而是始终通过统一平台接口读取受限分析策略。
 */
internal open class CaBaseRestrictedAnalysisService : CaRestrictedAnalysisService {
    override val isAnalysisRestricted: Boolean
        get() = false

    override val isRestrictedAnalysisAllowed: Boolean
        get() = true

    override fun rejectRestrictedAnalysis(): Nothing {
        throw IllegalStateException("Analysis is restricted by the current platform state.")
    }
}
