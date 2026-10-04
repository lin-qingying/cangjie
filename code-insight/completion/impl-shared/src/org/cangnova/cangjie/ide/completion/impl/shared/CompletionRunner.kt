package org.cangnova.cangjie.ide.completion.impl.shared

import com.intellij.openapi.progress.ProgressIndicator
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot

/**
 * 补全结果的下游接收端。
 *
 * sink 只接收**已渲染完成的快照**，不接受符号、类型或分析序列：
 * 这样即使 runner 被取消，已经发出的批次也可以安全地继续展示。
 */
interface CompletionResultSink {
    /**
     * 接收一批候选。
     *
     * [isLast] 表示本次补全结果已完整；实现方据此决定是否收尾 UI。
     */
    fun consume(batch: List<CompletionSnapshot>, isLast: Boolean)
}

/**
 * 候选区段。
 *
 * 区段把「同一类语义位置的候选」组织在一起：执行优先级由 [executionPriority] 决定，
 * 展示权重由候选自身的排序数据决定，两者不得混为一谈。
 */
interface CompletionSection {
    /** 区段标识，仅用于日志与测试断言。 */
    val id: String

    /**
     * 执行优先级，值越小越先执行。
     *
     * 局部与成员候选先于需要索引的顶层/包候选，使快速且相关的候选先可用。
     */
    val executionPriority: Int

    /**
     * 执行该区段并把结果推入 [sink]。
     *
     * 实现必须在 [indicator] 被取消时及时抛出取消异常，
     * 并且**不得**吞掉异常后返回空批次：空结果与失败必须可区分。
     */
    fun execute(request: CompletionRequest, sink: CompletionResultSink, indicator: ProgressIndicator)
}

/**
 * 补全执行器。
 *
 * 执行器只负责按优先级调度区段、分批推送结果与传播取消；
 * 它不解析语义，也不决定候选内容。
 */
interface CompletionRunner {
    /**
     * 串行执行给定区段。
     *
     * 返回时所有区段已完成或被取消；任何区段抛出的失败都向上传播。
     */
    fun runSerial(
        request: CompletionRequest,
        sections: List<CompletionSection>,
        sink: CompletionResultSink,
        indicator: ProgressIndicator,
    )
}

/**
 * 串行执行器。
 *
 * 这是默认且唯一启用的执行路径：并行执行必须先通过会话隔离、确定顺序、
 * 取消传播与串并行结果等价四项准入，否则不得启用。
 */
class SerialCompletionRunner : CompletionRunner {
    /**
     * 按执行优先级稳定排序后逐个执行；同一优先级保持注册顺序。
     */
    override fun runSerial(
        request: CompletionRequest,
        sections: List<CompletionSection>,
        sink: CompletionResultSink,
        indicator: ProgressIndicator,
    ) {
        val ordered = sections.sortedBy { it.executionPriority }
        ordered.forEachIndexed { index, section ->
            indicator.checkCanceled()
            section.execute(request, sink, indicator)
            // 最后一个区段负责通知收尾，保证 UI 在异常路径上也能区分「未完成」。
            if (index == ordered.lastIndex) sink.consume(emptyList(), isLast = true)
        }
        if (ordered.isEmpty()) sink.consume(emptyList(), isLast = true)
    }
}
