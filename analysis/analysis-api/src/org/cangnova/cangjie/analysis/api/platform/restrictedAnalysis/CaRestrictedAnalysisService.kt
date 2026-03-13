package org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project

/**
 * 受限分析服务（对齐 Kotlin 的 KotlinRestrictedAnalysisService）。
 *
 * 当平台处于受限模式（如 IntelliJ 的 dumb mode）时，
 * 分析结果可能不完整或不正确。
 *
 * 如果未注册此服务，则假定平台没有受限分析的概念。
 */
interface CaRestrictedAnalysisService {
    /** 平台当前是否处于受限分析模式 */
    val isAnalysisRestricted: Boolean

    /** 受限模式下是否允许分析 */
    val isRestrictedAnalysisAllowed: Boolean

    /** 拒绝受限分析时抛出异常 */
    fun rejectRestrictedAnalysis(): Nothing

    companion object {
        fun getInstance(project: Project): CaRestrictedAnalysisService? = project.serviceOrNull()
    }
}
