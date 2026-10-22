package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import com.intellij.openapi.progress.ProgressManager

/**
 * 按元素执行诊断收集，并把单个元素的异常隔离在该元素内。
 *
 * 该 helper 是 [org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.FileStructure]
 * 逐 structure element 收集的实际入口，单独暴露以便回归测试验证：
 * 某个元素失败后，后续元素仍然会被执行；失败同时记录到
 * [DiagnosticCollectionFailures]，不会变成静默 partial result。
 */
internal inline fun <T> forEachWithDiagnosticIsolation(
    elements: Iterable<T>,
    crossinline action: (T) -> Unit,
) {
    for (element in elements) {
        ProgressManager.checkCanceled()
        LLCfirDiagnosticVisitor.suppressAndLogExceptions {
            action(element)
        }
    }
}
