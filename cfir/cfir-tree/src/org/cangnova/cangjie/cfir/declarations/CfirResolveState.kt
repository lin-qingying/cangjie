/*
 * 仓颉 CFIR 懒加载解析状态管理。
 *
 * 该系统支持 IDE 模式下的按需解析（lazy resolve）：
 * - 单线程批处理：简单状态转移
 * - 多线程 IDE：并发安全，循环检测，等待机制
 */

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import java.util.concurrent.CountDownLatch

/**
 * CFIR 元素的懒加载解析状态。
 *
 * 密封类体系表示元素在 resolve 过程中的不同状态：
 * - `CfirResolvedToPhaseState(phase)` - 已解析到某阶段，当前无线程在处理
 * - `CfirInProcessOfResolvingToPhaseStateWithoutBarrier(phase)` - 当前线程正在解析到 phase
 * - `CfirInProcessOfResolvingToPhaseStateWithBarrier(phase)` - 当前线程正在解析，其他线程等待
 * - `CfirInProcessOfResolvingToJumpingPhaseState(phase)` - 跳转阶段解析，可能产生循环等待
 *
 * 参考 Kotlin K2 FIR 的 `FirResolveState` 设计。
 */
sealed class CfirResolveState {
    /**
     * 当前元素已解析到的阶段。
     *
     * 对于"正在解析"状态，返回前一个阶段。
     */
    abstract val resolvePhase: CfirResolvePhase

    /**
     * 返回当前 resolve state 的调试文本。
     */
    abstract override fun toString(): String
}

/**
 * 元素已解析到某阶段的状态。
 *
 * 表示：元素完全解析到 [resolvePhase]，当前没有任何线程在处理它。
 */
class CfirResolvedToPhaseState private constructor(
    /**
     * 元素已经完全解析到的阶段。
     */
    override val resolvePhase: CfirResolvePhase,
) : CfirResolveState() {
    /**
     * 各 resolve phase 对应的完成状态缓存。
     */
    companion object {
        /**
         * 缓存所有阶段的状态对象，避免每次阶段推进时重复分配小对象。
         */
        private val phases: List<CfirResolvedToPhaseState> =
            CfirResolvePhase.entries.map(::CfirResolvedToPhaseState)

        /**
         * 返回指定 [phase] 对应的完成状态实例。
         */
        operator fun invoke(phase: CfirResolvePhase): CfirResolvedToPhaseState =
            phases[phase.ordinal]
    }

    /**
     * 返回调试用的阶段状态文本。
     */
    override fun toString(): String = "ResolvedTo($resolvePhase)"
}

/**
 * 元素正在被解析的基类。
 *
 * 表示某个线程当前正在将元素解析到 [resolvingTo] 阶段。
 * 在这期间，[resolvePhase] 返回 [resolvingTo] 的前一个阶段。
 */
sealed class CfirInProcessOfResolvingToPhaseState : CfirResolveState() {
    /**
     * 目标解析阶段（当前线程正在解析到此阶段）。
     */
    abstract val resolvingTo: CfirResolvePhase

    /**
     * 当前已解析到的阶段 = resolvingTo 的前一个阶段。
     */
    override val resolvePhase: CfirResolvePhase get() = resolvingTo.previous
}

/**
 * 单线程模式：元素正在被解析，无其他线程等待。
 *
 * 此状态下其他线程若请求该元素的相同或更低阶段，应跳过等待，
 * 直接返回当前不完整的结果，以避免死锁。
 */
class CfirInProcessOfResolvingToPhaseStateWithoutBarrier private constructor(
    /**
     * 当前线程正在解析到的目标阶段。
     */
    override val resolvingTo: CfirResolvePhase,
) : CfirInProcessOfResolvingToPhaseState() {
    /**
     * 无等待屏障的 in-process 状态缓存。
     */
    companion object {
        /**
         * 每个非首阶段对应一个正在解析状态；[CfirResolvePhase.RAW_CFIR] 不能作为解析目标。
         */
        private val phases: List<CfirInProcessOfResolvingToPhaseState> =
            CfirResolvePhase.entries
                .drop(1) // 跳过 RAW_CFIR，因为它是首阶段
                .map(::CfirInProcessOfResolvingToPhaseStateWithoutBarrier)

        /**
         * 返回指定目标阶段的无屏障解析状态。
         *
         * [CfirResolvePhase.RAW_CFIR] 是初始状态，不能作为“正在解析到”的目标。
         */
        operator fun invoke(phase: CfirResolvePhase): CfirInProcessOfResolvingToPhaseState {
            require(phase != CfirResolvePhase.RAW_CFIR) {
                "Cannot resolve to ${CfirResolvePhase.RAW_CFIR} as it's the first phase"
            }
            return phases[phase.ordinal - 1]
        }
    }

    /**
     * 返回调试用的目标阶段文本。
     */
    override fun toString(): String = "ResolvingTo($resolvingTo)"
}

/**
 * 多线程模式：元素正在被解析，其他线程在等待。
 *
 * 包含一个 [barrier] (`CountDownLatch`)，当前解析线程完成后调用 `barrier.countDown()`，
 * 唤醒所有等待线程。
 */
class CfirInProcessOfResolvingToPhaseStateWithBarrier(
    /**
     * 当前线程正在解析到的目标阶段。
     */
    override val resolvingTo: CfirResolvePhase,
) : CfirInProcessOfResolvingToPhaseState() {
    /**
     * 其他线程通过此 latch 等待当前线程完成解析。
     */
    val barrier: CountDownLatch = CountDownLatch(1)

    init {
        require(resolvingTo != CfirResolvePhase.RAW_CFIR) {
            "Cannot resolve to ${CfirResolvePhase.RAW_CFIR} as it's the first phase"
        }
    }

    /**
     * 返回包含等待屏障语义的调试文本。
     */
    override fun toString(): String = "ResolvingToWithBarrier($resolvingTo)"
}

/**
 * 跳转阶段解析状态，用于处理 jumping phase 中的相互依赖。
 *
 * 当元素 A 解析 jumping phase 时，可能需要解析另一个元素 B 的同一 phase。
 * 而 B 可能又需要 A 的结果，形成循环依赖。
 *
 * 此状态通过 [waitingFor] 链记录这种依赖关系，用于检测和打破循环。
 */
class CfirInProcessOfResolvingToJumpingPhaseState(
    /**
     * 当前 jumping phase 解析的目标阶段。
     */
    override val resolvingTo: CfirResolvePhase,
) : CfirInProcessOfResolvingToPhaseState() {
    /**
     * 用于等待其他线程完成相关阶段的 latch。
     */
    val latch: CountDownLatch = CountDownLatch(1)

    /**
     * 若当前线程等待另一个线程的结果，[waitingFor] 指向那个线程的状态。
     * 用于检测循环依赖：若出现 A.waitingFor -> B.waitingFor -> A，则检测到循环。
     */
    @Volatile
    var waitingFor: CfirInProcessOfResolvingToJumpingPhaseState? = null

    /**
     * 返回 jumping phase 状态的调试文本。
     */
    override fun toString(): String = "ResolvingJumpingTo($resolvingTo)"
}

/**
 * 将 [CfirResolvePhase] 转换为对应的 [CfirResolvedToPhaseState]。
 */
fun CfirResolvePhase.asResolveState(): CfirResolvedToPhaseState =
    CfirResolvedToPhaseState(this)

/**
 * 初始化元素的 resolveState 为默认值（RAW_CFIR 阶段）。
 *
 * 由生成的 Builder `build()` 方法在构造 Impl 实例后自动调用，
 * 确保每个新创建的声明元素都有正确的初始 resolve state。
 */
@OptIn(ResolveStateAccess::class)
fun CfirElementWithResolveState.initDefaultResolveState() {
    resolveState = CfirResolvedToPhaseState(CfirResolvePhase.RAW_CFIR)
}

/**
 * 获取元素当前已解析到的阶段。
 *
 * 通过 [CfirElementWithResolveState.resolveState] 间接获取，
 * 避免直接暴露 resolveState 的内部细节。
 */
@OptIn(ResolveStateAccess::class)
val CfirElementWithResolveState.resolvePhase: CfirResolvePhase
    get() = resolveState.resolvePhase

/**
 * 将元素的 resolve state 替换为"已解析到 [newPhase]"。
 *
 * 这是一个简便方法，内部将 [resolveState] 设置为 [CfirResolvedToPhaseState]。
 * 仅在 resolve 管线中使用。
 */
@OptIn(ResolveStateAccess::class)
fun CfirElementWithResolveState.replaceResolvePhase(newPhase: CfirResolvePhase) {
    when (val currentState = resolveState) {
        is CfirResolvedToPhaseState -> {
            resolveState = CfirResolvedToPhaseState(newPhase)
        }

        is CfirInProcessOfResolvingToJumpingPhaseState -> {
            check(newPhase <= currentState.resolvingTo) {
                "Cannot publish phase $newPhase while $this is locked for ${currentState.resolvingTo}"
            }
            // jumping lock 与普通 write lock 一样，阶段发布由对应 unlock 负责。
        }

        is CfirInProcessOfResolvingToPhaseState -> {
            check(newPhase <= currentState.resolvingTo) {
                "Cannot publish phase $newPhase while $this is locked for ${currentState.resolvingTo}"
            }
            // 在 low-level lazy resolve 锁内，最终阶段发布由 unlock 统一完成。
            // 这里不能把 InProcess 状态提前覆写成 ResolvedTo，否则会破坏锁协议。
        }
    }
}

/**
 * 从 [CfirElement] 安全地获取 resolve phase。
 *
 * 若元素支持 resolve state（实现 `CfirElementWithResolveState`），
 * 则返回当前的 resolve phase；否则返回 null。
 */
val CfirElement.resolvePhaseOrNull: CfirResolvePhase?
    get() = (this as? CfirElementWithResolveState)?.resolvePhase

/**
 * 向访问 resolve state 的注解。
 *
 * 用 `@ResolveStateAccess` 标记的成员可以直接访问 `resolveState` 变量。
 */
@RequiresOptIn(
    message = "This member directly accesses the resolve state, which is internal to the " +
        "lazy resolve system. Use public APIs where possible.",
    level = RequiresOptIn.Level.WARNING,
)
/**
 * 标记允许直接访问 resolve state 的内部 API。
 */
annotation class ResolveStateAccess
