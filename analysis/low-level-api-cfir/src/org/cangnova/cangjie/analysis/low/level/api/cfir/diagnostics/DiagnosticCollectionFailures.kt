package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

/**
 * low-level 诊断收集中被隔离的异常记录。
 *
 * 单个 checker / structure element 失败时，收集流程必须继续处理其它元素；但“继续”不能
 * 等价于“没有失败”。这里把被隔离的异常显式记录下来，测试和真实项目报告可以断言
 * 收集期间没有发生任何被吞掉的异常，避免异常门禁失效。
 *
 * 记录按线程保存；当前 low-level 收集在单线程内顺序执行。
 */
object DiagnosticCollectionFailures {
    /**
     * 当前线程上尚未被消费的隔离异常。
     */
    private val failures: ThreadLocal<MutableList<Throwable>> = ThreadLocal.withInitial(::mutableListOf)

    /**
     * 记录一个被隔离的异常。
     */
    fun record(failure: Throwable) {
        failures.get().add(failure)
    }

    /**
     * 取出并清空当前线程的隔离异常。
     */
    fun drain(): List<Throwable> {
        val pending = failures.get()
        if (pending.isEmpty()) return emptyList()
        val result = pending.toList()
        pending.clear()
        return result
    }

    /**
     * 清空当前线程的隔离异常，供一次收集开始前重置状态。
     */
    fun clear() {
        failures.get().clear()
    }
}
