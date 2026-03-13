package org.cangjie.analysis.api.platform.restrictedAnalysis

/**
 * 受限分析异常（对齐 Kotlin 的 KaRestrictedAnalysisException）。
 *
 * 当受限分析模式下发生异常时，原始异常会被包装在此异常中。
 */
abstract class CaRestrictedAnalysisException(cause: Throwable) : RuntimeException(cause)
