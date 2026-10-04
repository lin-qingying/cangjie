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
 * macro construction 的阶段。
 *
 * construction 主流程是三步串行（见 [MacroConstructionService.expand]）：
 * 符号索引构建 → import 绑定 → 真实展开。三步都可能成为宏密集工程的瓶颈，
 * 只计时展开一步会让"卡在索引构建"被误读成"展开很快"。
 *
 * @property metricSuffix 指标名中的阶段段。
 */
enum class CfirMacroConstructionStage(val metricSuffix: String) {
    /** [buildMacroSymbolIndex]：收集并索引宏定义。 */
    SYMBOL_INDEX("symbolIndex"),

    /** [bindMacroImports]：把 pre-macro 文件的 import 绑定到 construction context。 */
    IMPORT_BINDING("importBinding"),

    /** [MacroConstructionService.expand]：执行真实展开（含 identity 实现）。 */
    EXPANSION("expansion"),
}

/**
 * macro construction 耗时观察者。
 *
 * 每个阶段结束后回调一次；宿主注册实现后才计时，未注册时经 [measureMacroConstructionStage]
 * 直接执行，各阶段零开销。
 *
 * 宿主侧约定：
 *
 * 1. 实现不得抛出异常影响构造，观察失败对调用方不可见；
 * 2. 时长以单调时钟纳秒传入，实现方自行决定单位与聚合方式。
 */
interface CfirMacroConstructionTimingObserver : CfirSessionComponent {
    /**
     * macro construction 的一个阶段结束。
     *
     * @param stage 本次执行的阶段
     * @param mode 构造运行模式（STRICT / DEGRADED）；只对 [CfirMacroConstructionStage.EXPANSION] 有意义
     * @param outcome 结果归类；只对 [CfirMacroConstructionStage.EXPANSION] 非 `null`
     * @param fileCount 本次覆盖的 pre-macro 文件数
     * @param surfaceCount 本次覆盖的宏 surface 数（即被展开的宏调用数）
     * @param elapsedNanos 本阶段的单调时钟耗时（纳秒）
     */
    fun onMacroConstructionFinished(
        stage: CfirMacroConstructionStage,
        mode: MacroConstructionService.Mode?,
        outcome: CfirMacroExpansionOutcome?,
        fileCount: Int,
        surfaceCount: Int,
        elapsedNanos: Long,
    )

    /**
     * macro construction 的一个阶段开始。
     *
     * 默认空实现：只有需要记录链路 span 的实现才覆写它。开始回调与 [onMacroConstructionFinished]
     * 在同一线程上严格成对。
     */
    fun onMacroConstructionStarted(
        stage: CfirMacroConstructionStage,
        mode: MacroConstructionService.Mode?,
        fileCount: Int,
        surfaceCount: Int,
    ) {
    }
}

/**
 * 在 [observer] 存在时对 [action] 计时并上报 [stage]；未注册观察者时直接执行 [action]。
 *
 * [action] 抛出异常时仍会上报已经消耗的时长，但不产生结果归类。
 */
inline fun <T> CfirMacroConstructionTimingObserver?.measureMacroConstructionStage(
    stage: CfirMacroConstructionStage,
    fileCount: Int,
    surfaceCount: Int,
    action: () -> T,
): T {
    if (this == null) {
        return action()
    }

    onMacroConstructionStarted(stage, null, fileCount, surfaceCount)
    val startedAt = TimeSource.Monotonic.markNow()
    try {
        return action()
    } finally {
        onMacroConstructionFinished(stage, null, null, fileCount, surfaceCount, startedAt.elapsedNow().inWholeNanoseconds)
    }
}

/**
 * 在 [observer] 存在时对 [action] 计时并上报展开阶段的结果归类；未注册观察者时直接执行 [action]。
 *
 * [action] 抛出异常时按 [CfirMacroExpansionOutcome.FAILED] 上报已消耗时长后原样重抛。
 */
inline fun CfirMacroConstructionTimingObserver?.measureMacroExpansion(
    mode: MacroConstructionService.Mode,
    fileCount: Int,
    surfaceCount: Int,
    action: () -> MacroConstructionResult,
): MacroConstructionResult {
    if (this == null) {
        return action()
    }

    onMacroConstructionStarted(CfirMacroConstructionStage.EXPANSION, mode, fileCount, surfaceCount)
    val startedAt = TimeSource.Monotonic.markNow()
    try {
        val result = action()
        onMacroConstructionFinished(
            CfirMacroConstructionStage.EXPANSION,
            mode,
            CfirMacroExpansionOutcome.of(result),
            fileCount,
            surfaceCount,
            startedAt.elapsedNow().inWholeNanoseconds,
        )
        return result
    } catch (e: Throwable) {
        onMacroConstructionFinished(
            CfirMacroConstructionStage.EXPANSION,
            mode,
            CfirMacroExpansionOutcome.FAILED,
            fileCount,
            surfaceCount,
            startedAt.elapsedNow().inWholeNanoseconds,
        )
        throw e
    }
}

/**
 * macro construction 耗时观察者；宿主未注册时为 `null`，构造路径据此跳过计时。
 */
val CfirSession.macroConstructionTimingObserverOrNull: CfirMacroConstructionTimingObserver? by CfirSession.nullableSessionComponentAccessor()

/**
 * 注册 macro construction 耗时观察者。
 */
fun CfirSession.registerMacroConstructionTimingObserver(observer: CfirMacroConstructionTimingObserver) {
    register(CfirMacroConstructionTimingObserver::class, observer)
}
