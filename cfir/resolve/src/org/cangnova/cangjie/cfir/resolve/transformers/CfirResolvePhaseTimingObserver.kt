package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import kotlin.time.TimeSource

/**
 * 一次阶段推进覆盖的工作量。
 *
 * 两条驱动路径的"处理单元"不同，指标必须分开计数，合成一个计数会失去意义：
 *
 * - 全量解析按文件集合推进，只知道文件数，逐声明统计需要额外的整棵树遍历；
 * - 按需解析按 designation 推进，文件数恒为 1，声明数才是随请求变化的负载。
 *
 * 因此约定：[files] 只由全量解析路径填写，[declarations] 只由按需解析路径填写，
 * 未适用的那个恒为 0。IMPORTS 例外：按需解析推进的是文件本身，而
 * `CfirFile` 本身也是 `CfirDeclaration`，故记入 [declarations]。
 */
data class CfirResolvePhaseWork(
    /**
     * 本次推进覆盖的 CFIR 文件数，仅全量解析路径填写。
     */
    val files: Int,
    /**
     * 本次推进覆盖的声明数，仅按需解析路径填写。
     */
    val declarations: Int,
)

/**
 * 语义解析阶段的耗时观察者。
 *
 * 语义解析按 [CfirResolvePhase] 顺序推进，驱动的循环有两条：
 *
 * 1. 全量解析：`cfir-entrypoint` 的 `CfirTotalResolveProcessor` 按阶段遍历整个文件集合；
 * 2. 按需解析：`analysis/low-level-api-cfir` 的 `LLCfirModuleLazyDeclarationResolver`
 *    按阶段推进单个 designation，其中 IMPORTS 由文件级 import transformer 单独推进。
 *
 * 本接口把每个阶段的执行事实交给上层记录，实现方只需关心"如何上报"，不参与解析语义。
 *
 * 宿主侧约定：
 *
 * 1. 未注册实现时解析路径零开销，调用方一律经 [measurePhase] 包装阶段执行；
 * 2. 实现不得抛出异常影响解析，观察失败对调用方不可见；
 * 3. 时长以单调时钟纳秒传入，实现方自行决定单位与聚合方式。
 */
interface CfirResolvePhaseTimingObserver : CfirSessionComponent {
    /**
     * 某个阶段开始执行。
     *
     * @param phase 当前阶段
     */
    fun onPhaseStarted(phase: CfirResolvePhase)

    /**
     * 某个阶段执行结束，无论其是否抛出异常都会回调。
     *
     * @param phase 当前阶段
     * @param work 本次阶段推进覆盖的文件数与声明数，见 [CfirResolvePhaseWork]
     * @param elapsedNanos 该阶段的单调时钟耗时（纳秒）
     */
    fun onPhaseFinished(phase: CfirResolvePhase, work: CfirResolvePhaseWork, elapsedNanos: Long)
}

/**
 * 在 [observer] 存在时对 [action] 计时，并以 [work] 作为本次阶段推进的工作量上报。
 *
 * 未注册观察者时直接执行 [action]：不取单调时钟、不引入包装对象，计时路径零开销。
 * [action] 抛出异常时仍会上报已经消耗的时长。
 */
inline fun CfirResolvePhaseTimingObserver?.measurePhase(
    phase: CfirResolvePhase,
    work: CfirResolvePhaseWork,
    action: () -> Unit,
) {
    if (this == null) {
        action()
        return
    }

    val startedAt = TimeSource.Monotonic.markNow()
    onPhaseStarted(phase)
    try {
        action()
    } finally {
        onPhaseFinished(phase, work, startedAt.elapsedNow().inWholeNanoseconds)
    }
}
