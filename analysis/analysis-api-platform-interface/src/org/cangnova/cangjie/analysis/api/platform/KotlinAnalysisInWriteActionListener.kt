package org.cangnova.cangjie.analysis.api.platform

import com.intellij.util.messages.Topic

/**
 * 监听写动作中的 analysis 进入/离开事件。
 *
 * 常规 Analysis API 约束要求分析运行在稳定的读上下文里；一旦在写动作中分析，
 * 宿主侧的 tree-change 处理器、cache invalidation 和重建流程就必须显式重置上下文，
 * 否则同一写动作内后续修改可能继续复用已过期状态。
 */
interface KotlinAnalysisInWriteActionListener {
    /**
     * 在写动作中即将进入分析时触发，发生在 `analyze(...)` 的用户动作执行之前。
     */
    fun onEnteringAnalysisInWriteAction()

    /**
     * 在写动作中的分析离开后触发，发生在 `analyze(...)` 的用户动作执行完成之后。
     */
    fun afterLeavingAnalysisInWriteAction()

    companion object {
        val TOPIC: Topic<KotlinAnalysisInWriteActionListener> = Topic(
            KotlinAnalysisInWriteActionListener::class.java,
            Topic.BroadcastDirection.TO_CHILDREN,
            true,
        )
    }
}
