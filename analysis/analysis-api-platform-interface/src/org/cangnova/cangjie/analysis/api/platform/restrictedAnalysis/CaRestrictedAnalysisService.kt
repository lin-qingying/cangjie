package org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project

/**
 * 受限分析服务。
 *
 * 该接口表示当前平台是否允许给出完整分析结果。IDE 的 dumb mode、
 * LSP 的增量同步窗口以及某些 Standalone 快照构建阶段，都可能进入受限模式。
 */
interface CaRestrictedAnalysisService {
    /**
     * 平台当前是否处于受限分析模式。
     */
    val isAnalysisRestricted: Boolean

    /**
     * 在受限模式下是否仍允许执行分析。
     */
    val isRestrictedAnalysisAllowed: Boolean

    /**
     * 当分析不被允许时抛出平台自定义异常。
     */
    fun rejectRestrictedAnalysis(): Nothing

    companion object {
        fun getInstance(project: Project): CaRestrictedAnalysisService? = project.serviceOrNull()
    }
}
