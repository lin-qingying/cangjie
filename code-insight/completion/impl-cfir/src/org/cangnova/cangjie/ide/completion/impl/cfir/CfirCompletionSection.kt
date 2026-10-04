package org.cangnova.cangjie.ide.completion.impl.cfir

import com.intellij.openapi.progress.ProgressIndicator
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.impl.shared.CompletionResultSink
import org.cangnova.cangjie.ide.completion.impl.shared.CompletionSection

/**
 * 仓颉补全区段的公共底座。
 *
 * 区段在**分析内**完成语义渲染，只把不可变快照推给 sink：
 * 这样分析结束后展示层仍可安全持有结果，不需要恢复 Analysis 会话。
 */
abstract class CfirCompletionSection(
    /**
     * 区段标识，仅用于日志与测试断言。
     */
    final override val id: String,
    /**
     * 执行优先级，值越小越先执行。
     */
    final override val executionPriority: Int,
) : CompletionSection {
    /**
     * 计算本区段的候选快照。
     *
     * 允许返回空列表表示「该位置没有此类候选」；但不得吞掉异常：
     * 失败必须以异常形式暴露，否则空结果会伪装成「没有候选」。
     */
    protected abstract fun compute(request: CompletionRequest): List<CompletionSnapshot>

    /**
     * 执行本区段并推入结果。
     */
    final override fun execute(
        request: CompletionRequest,
        sink: CompletionResultSink,
        indicator: ProgressIndicator,
    ) {
        indicator.checkCanceled()
        val snapshots = compute(request)
        indicator.checkCanceled()
        sink.consume(snapshots, isLast = false)
    }
}
