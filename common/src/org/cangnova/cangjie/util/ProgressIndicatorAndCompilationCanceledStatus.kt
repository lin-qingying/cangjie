package org.cangnova.cangjie.util

import com.intellij.openapi.progress.ProgressIndicatorProvider

interface CompilationCanceledStatus {
    fun checkCanceled()
}
object ProgressIndicatorAndCompilationCanceledStatus {
    private var canceledStatus: ThreadLocal<CompilationCanceledStatus?> = ThreadLocal.withInitial { null }

    @JvmStatic
    fun setCompilationCanceledStatus(newCanceledStatus: CompilationCanceledStatus?) {
        canceledStatus.set(newCanceledStatus)
    }

    @JvmStatic
    fun checkCanceled() {
        ProgressIndicatorProvider.checkCanceled()
        canceledStatus.get()?.checkCanceled()
    }
}
