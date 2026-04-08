package org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis

/**
 * 受限分析异常。
 *
 * 当平台处于受限分析模式时，底层异常会被包装成该异常向上抛出，
 * 以便调用方明确区分“语义错误”和“平台暂时不允许得到完整分析结果”。
 */
abstract class CaRestrictedAnalysisException(cause: Throwable) : RuntimeException(cause)
