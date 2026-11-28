package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import kotlin.time.TimeSource

/**
 * 诊断收集组件耗时观察者。
 *
 * 与 [CfirDiagnosticPassTimingObserver] 的分工：后者计量"一次遍历整体多久"（慢在 SEMA 还是
 * POST_SEMA），本观察者计量"这一次遍历里各组件分别多久"（慢在声明检查还是表达式检查）。
 * 两个粒度都不可省——整体耗时告诉你出了问题，逐组件耗时告诉你该改哪里。
 *
 * 计量边界是 [CheckerRunningDiagnosticCollectorVisitor] 把当前元素分发给单个组件的那一次调用。
 * 因为遍历对每个 CFIR 元素都会重复一次，同一组件的观测值自然是"该组件在本次收集中的累计"
 * 与"被调用的元素数"，两者相除即单次平均。
 *
 * 设计沿用仓内既有接缝：观察者挂在 session 上，宿主注册实现后才计时，未注册时遍历路径
 * 只多一次可空判等，不取单调时钟。
 */
interface CfirCheckerComponentTimingObserver : CfirSessionComponent {
    /**
     * 一个元素在单个诊断组件上的检查结束。
     *
     * @param kind 本次执行的组件种类
     * @param elapsedNanos 本次检查的单调时钟耗时（纳秒）
     */
    fun onCheckerComponentFinished(kind: CfirCheckerComponentKind, elapsedNanos: Long)
}

/**
 * 在 [observer] 存在时对 [action] 计时并上报 [kind]；未注册观察者时直接执行 [action]。
 *
 * [action] 抛出异常时仍会上报已经消耗的时长：失败的检查同样是耗时，值得看见。
 */
inline fun <T> CfirCheckerComponentTimingObserver?.measureCheckerComponent(
    kind: CfirCheckerComponentKind,
    action: () -> T,
): T {
    if (this == null) {
        return action()
    }

    val startedAt = TimeSource.Monotonic.markNow()
    try {
        return action()
    } finally {
        onCheckerComponentFinished(kind, startedAt.elapsedNow().inWholeNanoseconds)
    }
}

/**
 * 诊断收集组件耗时观察者；宿主未注册时为 `null`，遍历路径据此跳过计时。
 */
val CfirSession.checkerComponentTimingObserverOrNull: CfirCheckerComponentTimingObserver? by CfirSession.nullableSessionComponentAccessor()

/**
 * 注册诊断收集组件耗时观察者。
 */
fun CfirSession.registerCheckerComponentTimingObserver(observer: CfirCheckerComponentTimingObserver) {
    register(CfirCheckerComponentTimingObserver::class, observer)
}