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
     * 该区段是否属于**延迟阶段**。
     *
     * 延迟阶段的候选只有在快速阶段全部跑完、且请求仍未被取消时才会计算。
     * 典型成员是需要索引或跨模块查询的区段：它们慢，但不该拖住用户已经能看到的结果。
     *
     * 阶段划分与**平台何时开始延迟阶段**是两件事：本层只保证「快速阶段的批次先于
     * 延迟阶段」，真正的延迟时长由宿主决定（它才知道用户是否还在继续输入）。
     */
    val isDelayed: Boolean get() = false

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
     * 先跑快速阶段，再跑延迟阶段；每阶段内部按执行优先级稳定排序，同一优先级保持注册顺序。
     *
     * **延迟阶段在进入前重新检查取消**：用户已经继续输入时，前半段结果早已送达并可以
     * 直接展示，此时再去算索引候选纯属浪费——而且会把一次被取消的请求拖成一次慢请求。
     *
     * 收尾只在**全部完成后**通知一次：延迟阶段被取消时整个调用抛取消异常，
     * 由调用方区分「未完成」与「完成」，不会出现「已经收尾了但候选还在来」。
     */
    override fun runSerial(
        request: CompletionRequest,
        sections: List<CompletionSection>,
        sink: CompletionResultSink,
        indicator: ProgressIndicator,
    ) {
        val ordered = sections.sortedBy { it.executionPriority }
        val (fast, delayed) = ordered.partition { !it.isDelayed }
        runPhase(fast, request, sink, indicator)
        runPhase(delayed, request, sink, indicator)
        sink.consume(emptyList(), isLast = true)
    }

    /**
     * 执行一个阶段。
     *
     * 每个区段执行前检查取消：区段内部的取消检查由区段自己负责（它才知道自己的循环在哪）。
     */
    private fun runPhase(
        sections: List<CompletionSection>,
        request: CompletionRequest,
        sink: CompletionResultSink,
        indicator: ProgressIndicator,
    ) {
        for (section in sections) {
            indicator.checkCanceled()
            section.execute(request, sink, indicator)
        }
    }
}
