package org.cangnova.cangjie.util

import com.intellij.openapi.progress.ProgressIndicatorProvider

/**
 * 编译取消状态检查接口。
 */
interface CompilationCanceledStatus {
    /**
     * 检查当前编译是否应被取消。
     */
    fun checkCanceled()
}
/**
 * IntelliJ ProgressIndicator 与编译取消状态的统一入口。
 */
object ProgressIndicatorAndCompilationCanceledStatus {
    /**
     * 当前线程绑定的编译取消状态。
     */
    private var canceledStatus: ThreadLocal<CompilationCanceledStatus?> = ThreadLocal.withInitial { null }

    /**
     * 设置当前线程使用的编译取消状态。
     */
    @JvmStatic
    fun setCompilationCanceledStatus(newCanceledStatus: CompilationCanceledStatus?) {
        canceledStatus.set(newCanceledStatus)
    }

    /**
     * 同时检查 IntelliJ progress cancellation 和编译取消状态。
     */
    @JvmStatic
    fun checkCanceled() {
        ProgressIndicatorProvider.checkCanceled()
        canceledStatus.get()?.checkCanceled()
    }
}
