package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnostic

/**
 * 调用解析尝试的公开视图标记。
 *
 * 对齐 Kotlin Analysis API 的 `KaCallResolutionAttempt`:
 * 该接口用作"解析过程产物"的统一锚点,具体子类型由实现层提供;
 * 在公开层只保证它是受 lifetime 管理的对象。
 */
sealed interface CaCallResolutionAttempt : CaLifetimeOwner

/**
 * 调用解析失败时保留的候选调用与诊断。
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaCallResolutionError : CaCallResolutionAttempt {
    /** 描述本次调用解析失败的诊断。 */
    val diagnostic: CaDiagnostic

    /** 调用解析过程中真实参与决策的候选；无法恢复时为空。 */
    val candidateCalls: List<CaCall>
}

/**
 * 调用解析成功时选中的唯一调用。
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaCallResolutionSuccess : CaCallResolutionAttempt {
    /** 成功解析得到的调用。 */
    val call: CaCall
}

/**
 * 获取解析尝试对应的公开调用列表。
 */
val CaCallResolutionAttempt.calls: List<CaCall>
    get() = when (this) {
        is CaCallResolutionSuccess -> listOf(call)
        is CaCallResolutionError -> candidateCalls
    }

/**
 * 符号解析尝试的公开视图标记。
 *
 * 对齐 Kotlin Analysis API 的 `KaSymbolResolutionAttempt`:
 * 与 [CaCallResolutionAttempt] 类似,只作为符号级解析过程的稳定锚点。
 */
sealed interface CaSymbolResolutionAttempt : CaLifetimeOwner
