package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import kotlin.time.TimeSource

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
     * @param fileCount 本次阶段执行覆盖的 CFIR 文件数：全量解析为该阶段的文件集合大小，
     *   按需解析为 designation 所在文件数（无文件的 synthetic 元素为 0）
     * @param elapsedNanos 该阶段的单调时钟耗时（纳秒）
     */
    fun onPhaseFinished(phase: CfirResolvePhase, fileCount: Int, elapsedNanos: Long)
}

/**
 * 在 [observer] 存在时对 [action] 计时，并以 [fileCount] 作为本次阶段执行覆盖的文件数上报。
 *
 * 未注册观察者时直接执行 [action]：不取单调时钟、不引入包装对象，计时路径零开销。
 * [action] 抛出异常时仍会上报已经消耗的时长。
 */
inline fun CfirResolvePhaseTimingObserver?.measurePhase(
    phase: CfirResolvePhase,
    fileCount: Int,
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
        onPhaseFinished(phase, fileCount, startedAt.elapsedNow().inWholeNanoseconds)
    }
}
