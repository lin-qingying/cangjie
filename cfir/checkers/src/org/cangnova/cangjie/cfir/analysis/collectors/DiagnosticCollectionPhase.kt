package org.cangnova.cangjie.cfir.analysis.collectors

/** 诊断遍历的执行阶段；后续分析只有在当前诊断根通过 Sema 后才进入。 */
enum class DiagnosticCollectionPhase {
    SEMA,
    POST_SEMA,
}
