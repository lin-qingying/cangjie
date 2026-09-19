package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageFeatureSupportStatus
import org.cangnova.cangjie.featureSupportStatus
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * CFIR checker 统一 feature gate。
 *
 * 与 Kotlin FIR 的 `requireFeatureSupport` 对齐：同一个入口负责读取
 * [LanguageFeatureSupportStatus]、保留完整 settings 诊断载荷，并由调用方提供
 * 语义节点位置。checker 不得重新比较 language/API version。
 */
internal fun CheckerContext.requireFeatureSupport(
    feature: LanguageFeature,
    source: AbstractCjSourceElement?,
    reporter: DiagnosticReporter,
): Boolean {
    val status = languageVersionSettings.featureSupportStatus(feature)
    if (status == LanguageFeatureSupportStatus.SUPPORTED) return true
    reporter.reportOn(
        source = source ?: containingFileSymbol?.cfir?.source,
        factory = CfirErrors.UNSUPPORTED_FEATURE,
        a = feature to languageVersionSettings,
    )
    return false
}
