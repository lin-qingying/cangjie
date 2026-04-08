package org.cangnova.cangjie.analysis.api.cfir.resolve

import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic

/**
 * 诊断快照按 Analysis API 的 checker 维度分桶。
 *
 * low-level 层必须先把一次分析得到的诊断固化成稳定快照，
 * 上层才不会继续感知 reporter 注册顺序或底层 collector 实现差异。
 */
class DiagnosticBuckets(
    private val defaultDiagnostics: List<CjPsiDiagnostic>,
    private val extraDiagnostics: List<CjPsiDiagnostic>,
    private val experimentalDiagnostics: List<CjPsiDiagnostic>,
) {
    fun forFilter(filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        return buildList {
            if (filter.runDefaultCheckers) addAll(defaultDiagnostics)
            if (filter.runExtraCheckers) addAll(extraDiagnostics)
            if (filter.runExperimentalCheckers) addAll(experimentalDiagnostics)
        }
    }
}
