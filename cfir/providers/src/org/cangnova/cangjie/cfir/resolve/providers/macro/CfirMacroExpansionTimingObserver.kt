package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import kotlin.time.TimeSource

/**
 * macro construction 的结果归类。
 *
 * 与 [MacroConstructionResult] 的五个分支一一对应；IDE 卡顿排查关心的是
 * "降级了多少次"与"真正展开成功了多少次"，因此每种结果都单独计数。
 *
 * @property metricSuffix 指标名中的结果段。
 */
enum class CfirMacroExpansionOutcome(val metricSuffix: String) {
    /** 完整展开成功。 */
    SUCCESS("success"),

    /** 降级成功：文件含 typed error placeholder，IDE / analysis 可继续工作。 */
    DEGRADED("degraded"),

    /** construction 失败且没有可注册文件。 */
    FAILED("failed"),

    /** executor 不可用导致无法完成真实展开。 */
    EXECUTOR_UNAVAILABLE("executorUnavailable"),

    /** construction 被语义或环境条件阻塞。 */
    BLOCKED("blocked"),

    ;

    companion object {
        /** 把 [MacroConstructionResult] 映射为结果归类。 */
        fun of(result: MacroConstructionResult): CfirMacroExpansionOutcome = when (result) {
            is MacroConstructionResult.Success -> SUCCESS
            is MacroConstructionResult.Degraded -> DEGRADED
            is MacroConstructionResult.Failed -> FAILED
            is MacroConstructionResult.ExecutorUnavailable -> EXECUTOR_UNAVAILABLE
            is MacroConstructionResult.Blocked -> BLOCKED
        }
    }
}

/**
 * macro construction（`MacroConstructionService.expand`）耗时观察者。
 *
 * 每次 construction 结束后回调一次，携带结果归类、文件数与宏 surface 数；
 * 宿主注册实现后才计时，未注册时经 [measureMacroExpansion] 直接展开。
 *
 * 宿主侧约定：
 *
 * 1. 实现不得抛出异常影响展开，观察失败对调用方不可见；
 * 2. 时长以单调时钟纳秒传入，实现方自行决定单位与聚合方式。
 */
interface CfirMacroExpansionTimingObserver : CfirSessionComponent {
    /**
     * 一次 macro construction 结束。
     *
     * @param mode 构造运行模式（STRICT / DEGRADED）
     * @param outcome 结果归类
     * @param fileCount 本次 construction 覆盖的 pre-macro 文件数
     * @param surfaceCount 本次 construction 覆盖的宏 surface 数（即被展开的宏调用数）
     * @param elapsedNanos 本次 construction 的单调时钟耗时（纳秒）
     */
    fun onMacroExpansionFinished(
        mode: MacroConstructionService.Mode,
        outcome: CfirMacroExpansionOutcome,
        fileCount: Int,
        surfaceCount: Int,
        elapsedNanos: Long,
    )
}

/**
 * 在 [observer] 存在时对 [action] 计时并上报；未注册观察者时直接执行 [action]。
 *
 * [action] 抛出异常时不上报结果归类（没有结果），但已消耗的时长仍会上报为 [CfirMacroExpansionOutcome.FAILED]。
 */
inline fun CfirMacroExpansionTimingObserver?.measureMacroExpansion(
    mode: MacroConstructionService.Mode,
    fileCount: Int,
    surfaceCount: Int,
    action: () -> MacroConstructionResult,
): MacroConstructionResult {
    if (this == null) {
        return action()
    }

    val startedAt = TimeSource.Monotonic.markNow()
    try {
        val result = action()
        onMacroExpansionFinished(mode, CfirMacroExpansionOutcome.of(result), fileCount, surfaceCount, startedAt.elapsedNow().inWholeNanoseconds)
        return result
    } catch (e: Throwable) {
        onMacroExpansionFinished(mode, CfirMacroExpansionOutcome.FAILED, fileCount, surfaceCount, startedAt.elapsedNow().inWholeNanoseconds)
        throw e
    }
}

/**
 * macro construction 耗时观察者；宿主未注册时为 `null`，展开路径据此跳过计时。
 */
val CfirSession.macroExpansionTimingObserverOrNull: CfirMacroExpansionTimingObserver? by CfirSession.nullableSessionComponentAccessor()

/**
 * 注册 macro construction 耗时观察者。
 */
fun CfirSession.registerMacroExpansionTimingObserver(observer: CfirMacroExpansionTimingObserver) {
    register(CfirMacroExpansionTimingObserver::class, observer)
}
