package org.cangnova.cangjie.cfir.diagnostics

/**
 * 诊断提交期裁决：决定一条已到提交时机的诊断是否真正输出。
 *
 * 编译管线（`PendingDiagnosticsReporterImpl`）与 IDE 低级 API（`LLCfirDiagnosticReporter`）
 * 的 pending reporter 必须共用同一策略，否则同一份源码在 CLI 与 IDE 中会得到不同的诊断集合。
 *
 * 与 Kotlin K2 的偏离：K2 的 `PendingDiagnosticsCollectorWithSuppress` 与 `LLFirDiagnosticReporter`
 * 在提交时只处理 suppress，没有对应的提交期裁决。仓颉官方 `CompilerDiagnosticHandler::HandleDiagnose`
 * 发出诊断前会用 `HasPrevDiag(主提示起点, 消息)` 丢弃重复诊断，诊断引擎对同一位置也只保留更具体的一条，
 * 因此 CFIR 需要这一层 Kotlin 没有的抽象。
 */
interface DiagnosticCommitPolicy {
    /**
     * 判断 [diagnostic] 是否与同文件已提交的某条诊断等价，等价时不再输出。
     */
    fun isDuplicateOfCommitted(diagnostic: CjDiagnostic, committed: Sequence<CjDiagnostic>): Boolean

    /**
     * 判断 [diagnostic] 是否被同文件同位置上更具体的诊断取代。
     *
     * @param pending 同文件仍待提交的诊断
     * @param committed 同文件已提交的诊断
     */
    fun isSupersededByMoreSpecific(
        diagnostic: CjDiagnostic,
        pending: Sequence<CjDiagnostic>,
        committed: Sequence<CjDiagnostic>,
    ): Boolean
}
