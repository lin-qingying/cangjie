package org.cangnova.cangjie.cfir.resolve.inference

import java.util.concurrent.atomic.AtomicLong
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate

/**
 * retained completion 的只读运行证据。span 和序号不参与 selection、lease、replay 或 Source 决策。
 * 仅由 cangjie.debug.formalCallIdentity 启用；日志保留真实对象身份和 JVM 调用栈。
 */
internal object CfirRetainedCompletionTrace {
    internal class Span(val id: Long, val kind: String)
    private val sequence = AtomicLong()
    private val spans = ThreadLocal.withInitial { ArrayDeque<Span>() }

    fun identity(value: Any?): String =
        if (value == null) "null" else value.javaClass.simpleName + "@" + System.identityHashCode(value).toString(16)

    fun candidate(value: Candidate?): String {
        if (value == null) return "null"
        val scope = value.implicitTypeComputationScope
        return identity(value) + "{target=" + identity(value.symbol) +
            ",site=" + identity(value.callInfo.callSite) + ",kind=" + value.callInfo.callKind +
            ",scope=" + identity(scope) + ",mode=" + scope?.mode + ",graph=" + identity(scope?.graph) +
            ",resource=" + identity(scope?.graph?.resource) +
            ",retained=" +
            value.retainedChildSessions.joinToString(prefix = "[", postfix = "]") { identity(it) } +
            ",queued=" + value.postponedPCLACalls.size + "}"
    }

    fun stack(): String = Throwable().stackTrace.take(64).joinToString(" <- ")

    fun event(kind: String, details: () -> String) {
        if (!java.lang.Boolean.getBoolean("cangjie.debug.formalCallIdentity")) return
        val current = spans.get()
        System.err.println("RETAINED_ORDER seq=" + sequence.incrementAndGet() +
            " nano=" + System.nanoTime() + " thread=" + identity(Thread.currentThread()) +
            " event=" + kind + " path=" + current.joinToString("/") { it.kind + ":" + it.id } +
            " " + details())
    }

    fun enter(kind: String, details: () -> String): Span? {
        if (!java.lang.Boolean.getBoolean("cangjie.debug.formalCallIdentity")) return null
        val current = spans.get()
        val span = Span(sequence.incrementAndGet(), kind)
        current.addLast(span)
        event(kind + "_ENTER") { "span=" + span.id + " " + details() }
        return span
    }

    /** finally 只清理诊断栈；不会把退出 span 认定为完成证明。 */
    fun leave(span: Span?) {
        if (span == null) return
        event(span.kind + "_LEAVE") { "span=" + span.id }
        val current = spans.get()
        if (current.lastOrNull() === span) current.removeLast()
        else {
            event("TRACE_SPAN_MISMATCH") { "span=" + span.id }
            current.remove(span)
        }
        if (current.isEmpty()) spans.remove()
    }
}
