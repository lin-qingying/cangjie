package org.cangjie.analysis.api.impl.base.permissions

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationListener
import com.intellij.openapi.application.ApplicationManager

/**
 * 写操作检查器（对齐 Kotlin 的 KaBaseWriteActionStartedChecker）。
 *
 * 确保在 analyze() 调用内部不会启动写操作（除非最外层的 analyze() 本身就在写操作中）。
 */
internal class CaBaseWriteActionStartedChecker(parentDisposable: Disposable) {
    private val hasEncounteredIllegalWriteAction = ThreadLocal.withInitial { false }

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

    fun beforeEnteringAnalysis() {
        currentAnalyzeCallDepth.set(currentAnalyzeCallDepth.get() + 1)
    }

    fun afterLeavingAnalysis() {
        currentAnalyzeCallDepth.set(currentAnalyzeCallDepth.get() - 1)

        if (hasEncounteredIllegalWriteAction.get()) {
            hasEncounteredIllegalWriteAction.remove()
            throw WriteActionStartedInAnalysisContextException()
        }
    }
}

private class WriteActionStartedInAnalysisContextException : IllegalStateException(
    "A write action should never be executed inside an analysis context (i.e. an `analyze` call)."
)
