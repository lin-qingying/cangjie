package org.cangjie.analysis.api.impl.base.restrictedAnalysis

import org.cangjie.analysis.api.platform.restrictedAnalysis.CaRestrictedAnalysisException

/**
 * 受限分析异常的基础实现（对齐 Kotlin 的 KaBaseRestrictedAnalysisException）。
 */
internal class CaBaseRestrictedAnalysisException(cause: Throwable) : CaRestrictedAnalysisException(cause)
