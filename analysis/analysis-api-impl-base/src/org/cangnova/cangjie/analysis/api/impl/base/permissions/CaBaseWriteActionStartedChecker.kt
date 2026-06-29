package org.cangnova.cangjie.analysis.api.impl.base.permissions

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationListener
import com.intellij.openapi.application.ApplicationManager

/**
 * 写操作检查器（对齐 Kotlin 的 KaBaseWriteActionStartedChecker）。
 *
 * 确保在 analyze() 调用内部不会启动写操作（除非最外层的 analyze() 本身就在写操作中）。
 */
internal class CaBaseWriteActionStartedChecker(parentDisposable: Disposable) {
    /**
     * 当前线程是否在 analysis context 内遇到非法写动作。
     */
    private val hasEncounteredIllegalWriteAction = ThreadLocal.withInitial { false }

    /**
     * 当前线程嵌套 analyze 调用深度。
     */
    private val currentAnalyzeCallDepth = ThreadLocal.withInitial { 0 }

    init {
        val listener = object : ApplicationListener {
            override fun beforeWriteActionStart(action: Any) {
                if (ApplicationManager.getApplication().isWriteAccessAllowed) return

                if (currentAnalyzeCallDepth.get() > 0) {
                    hasEncounteredIllegalWriteAction.set(true)
                }
            }
        }
        ApplicationManager.getApplication().addApplicationListener(listener, parentDisposable)
    }

    /**
     * 进入 analysis context 前递增调用深度。
     */
    fun beforeEnteringAnalysis() {
        currentAnalyzeCallDepth.set(currentAnalyzeCallDepth.get() + 1)
    }

    /**
     * 离开 analysis context 后递减调用深度，并在发现非法写动作时失败。
     */
    fun afterLeavingAnalysis() {
        currentAnalyzeCallDepth.set(currentAnalyzeCallDepth.get() - 1)

        if (hasEncounteredIllegalWriteAction.get()) {
            hasEncounteredIllegalWriteAction.remove()
            throw WriteActionStartedInAnalysisContextException()
        }
    }
}

/**
 * analysis context 内启动写动作时抛出的异常。
 */
private class WriteActionStartedInAnalysisContextException : IllegalStateException(
    "A write action should never be executed inside an analysis context (i.e. an `analyze` call)."
)
