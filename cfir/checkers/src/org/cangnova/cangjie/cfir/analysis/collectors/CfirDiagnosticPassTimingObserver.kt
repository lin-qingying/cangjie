package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import kotlin.time.TimeSource

/**
 * 诊断遍历阶段耗时观察者。
 *
 * 一次 `collectDiagnostics` 会跑两段遍历：`SEMA` 常规检查，以及在文件无错误时才继续的
 * `POST_SEMA` 后续检查。分段计时回答的是"慢在常规检查还是慢在后续检查"——两者的优化
 * 方向完全不同，合并成一个耗时会让人误判。
 *
 * 阶段边界就是 [AbstractDiagnosticCollector] 的 `runDiagnosticPass`，与
 * `CfirTotalResolveProcessor` 逐 phase 上报是同一种设计：观察者挂在 session 上，
 * 宿主注册实现后才计时，未注册时遍历路径零开销。
 */
interface CfirDiagnosticPassTimingObserver : CfirSessionComponent {
    /**
     * 一次诊断遍历阶段结束。
     *
     * @param phase 本次执行的阶段
     * @param elapsedNanos 本阶段的单调时钟耗时（纳秒）
     */
    fun onDiagnosticPassFinished(phase: DiagnosticCollectionPhase, elapsedNanos: Long)

    /**
     * 一次诊断遍历阶段开始。
     *
     * 默认空实现：只有需要记录链路 span 的实现才覆写它。开始回调与 [onDiagnosticPassFinished]
     * 在同一线程上严格成对。
     */
    fun onDiagnosticPassStarted(phase: DiagnosticCollectionPhase) {
    }
}

/**
 * 在 [observer] 存在时对 [action] 计时并上报 [phase]；未注册观察者时直接执行 [action]。
 *
 * [action] 抛出异常时仍会上报已经消耗的时长：失败的遍历同样是耗时，值得看见。
 */
inline fun <T> CfirDiagnosticPassTimingObserver?.measureDiagnosticPass(
    phase: DiagnosticCollectionPhase,
    action: () -> T,
): T {
    if (this == null) {
        return action()
    }

    onDiagnosticPassStarted(phase)
    val startedAt = TimeSource.Monotonic.markNow()
    try {
        return action()
    } finally {
        onDiagnosticPassFinished(phase, startedAt.elapsedNow().inWholeNanoseconds)
    }
}

/**
 * 诊断遍历阶段耗时观察者；宿主未注册时为 `null`，遍历路径据此跳过计时。
 */
val CfirSession.diagnosticPassTimingObserverOrNull: CfirDiagnosticPassTimingObserver? by CfirSession.nullableSessionComponentAccessor()

/**
 * 注册诊断遍历阶段耗时观察者。
 */
fun CfirSession.registerDiagnosticPassTimingObserver(observer: CfirDiagnosticPassTimingObserver) {
    register(CfirDiagnosticPassTimingObserver::class, observer)
}
