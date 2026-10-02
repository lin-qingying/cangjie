package org.cangnova.cangjie.cfir.builder

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
 * raw CFIR 构建流水线的阶段。
 *
 * "源码 → raw CFIR" 由解析与转换两步组成，两步必须分开记录：IDE 首开文件卡顿时，
 * 只有区分是解析慢还是转换慢才知道该优化词法/语法还是 CFIR 构造。
 *
 * @property metricSuffix 指标名中的阶段段。
 */
enum class CfirRawBuildStage(val metricSuffix: String) {
    /** 源码解析为语法树。 */
    PARSE("parse"),

    /** 语法树转换为 raw CFIR。 */
    CONVERT("convert"),
}

/**
 * raw CFIR 构建耗时观察者。
 *
 * 每次 PSI 或 LightTree 文件级 raw 构建的每个阶段结束后回调一次；宿主注册实现后才计时，
 * 未注册时经 [measureRawBuild] 直接执行，构建路径零开销。
 *
 * 两条来源的阶段覆盖并不对称，这是事实而非缺陷：LightTree 路径的解析由本项目完成，
 * PSI 路径的解析由 IntelliJ 平台的文件加载完成，本项目无法在其外侧取到耗时，
 * 因此 PSI 路径只上报 [CfirRawBuildStage.CONVERT]。
 *
 * 宿主侧约定：
 *
 * 1. 实现不得抛出异常影响构建，观察失败对调用方不可见；
 * 2. 时长以单调时钟纳秒传入，实现方自行决定单位与聚合方式；
 * 3. 只有真正执行该阶段时才回调，命中文件缓存的复用不产生采样。
 */
interface CfirRawBuildTimingObserver : CfirSessionComponent {
    /**
     * raw CFIR 构建的一个阶段结束。
     *
     * @param source 构建所用的前端来源
     * @param stage 本次执行的阶段
     * @param bodyBuildingMode 构建时的 body 策略（立即构建 / 延迟构建）；解析阶段不适用
     * @param elapsedNanos 本阶段的单调时钟耗时（纳秒）
     */
    fun onRawBuildFinished(source: CfirRawBuildSource, stage: CfirRawBuildStage, bodyBuildingMode: BodyBuildingMode?, elapsedNanos: Long)
}

/**
 * 在 [observer] 存在时对 [action] 计时并上报；未注册观察者时直接执行 [action]。
 *
 * [bodyBuildingMode] 只对 [CfirRawBuildStage.CONVERT] 有意义，解析阶段传 `null`。
 * [action] 抛出异常时仍会上报已经消耗的时长。
 */
inline fun <T> CfirRawBuildTimingObserver?.measureRawBuild(
    source: CfirRawBuildSource,
    stage: CfirRawBuildStage,
    bodyBuildingMode: BodyBuildingMode? = null,
    action: () -> T,
): T {
    if (this == null) {
        return action()
    }

    val startedAt = TimeSource.Monotonic.markNow()
    try {
        return action()
    } finally {
        onRawBuildFinished(source, stage, bodyBuildingMode, startedAt.elapsedNow().inWholeNanoseconds)
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
