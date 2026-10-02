package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import kotlin.time.TimeSource

/**
 * raw CFIR 构建的前端来源。
 *
 * IDE 首开文件走哪条路径由 LightTree 可用性决定（见 `CompilerConfiguration.useLightTree`），
 * 两条路径的耗时特征不同，必须分开统计，不能合并成一个数字。
 *
 * @property metricSuffix 指标名中的来源段。
 */
enum class CfirRawBuildSource(val metricSuffix: String) {
    /** PSI 路径：`PsiRawCfirBuilder`（Analysis API lazy resolve、CLI 默认路径）。 */
    PSI("psi"),

    /** LightTree 路径：`LightTreeRawCfirDeclarationBuilder`（批量/首开文件路径）。 */
    LIGHT_TREE("lightTree"),
}

/**
 * raw CFIR 构建耗时观察者。
 *
 * 每次 PSI 或 LightTree 文件级 raw 构建结束后回调一次；宿主注册实现后才计时，
 * 未注册时经 [measureRawBuild] 直接执行构建，构建路径零开销。
 *
 * 宿主侧约定：
 *
 * 1. 实现不得抛出异常影响构建，观察失败对调用方不可见；
 * 2. 时长以单调时钟纳秒传入，实现方自行决定单位与聚合方式；
 * 3. 只有真正执行构建时才回调，命中文件缓存的复用不产生采样。
 */
interface CfirRawBuildTimingObserver : CfirSessionComponent {
    /**
     * 一次 raw CFIR 构建结束。
     *
     * @param source 构建所用的前端来源
     * @param bodyBuildingMode 构建时的 body 策略（立即构建 / 延迟构建）
     * @param elapsedNanos 本次构建的单调时钟耗时（纳秒）
     */
    fun onRawBuildFinished(source: CfirRawBuildSource, bodyBuildingMode: BodyBuildingMode, elapsedNanos: Long)
}

/**
 * 在 [observer] 存在时对 [action] 计时并上报；未注册观察者时直接执行 [action]。
 *
 * [action] 抛出异常时仍会上报已经消耗的时长。
 */
inline fun CfirRawBuildTimingObserver?.measureRawBuild(
    source: CfirRawBuildSource,
    bodyBuildingMode: BodyBuildingMode,
    action: () -> CfirFile,
): CfirFile {
    if (this == null) {
        return action()
    }

    val startedAt = TimeSource.Monotonic.markNow()
    try {
        return action()
    } finally {
        onRawBuildFinished(source, bodyBuildingMode, startedAt.elapsedNow().inWholeNanoseconds)
    }
}

/**
 * raw 构建耗时观察者；宿主未注册时为 `null`，构建路径据此跳过计时。
 */
val CfirSession.rawBuildTimingObserverOrNull: CfirRawBuildTimingObserver? by CfirSession.nullableSessionComponentAccessor()

/**
 * 注册 raw 构建耗时观察者。
 */
fun CfirSession.registerRawBuildTimingObserver(observer: CfirRawBuildTimingObserver) {
    register(CfirRawBuildTimingObserver::class, observer)
}
