package org.cangnova.cangjie.phaser

/**
 * Phase 执行动作（对齐 K2 的 Action）
 */
enum class BeforeOrAfter {
    /**
     * 表示 action 在 phase 主体执行前触发。
     */
    BEFORE,

    /**
     * 表示 action 在 phase 主体执行后触发。
     */
    AFTER,
}

/**
 * 传递给 phase action 的运行时状态快照。
 */
data class ActionState(
    /**
     * 当前编译流水线使用的 phase 配置。
     */
    val config: PhaseConfig,
    /**
     * 正在执行前置或后置 action 的命名 phase。
     */
    val phase: AnyNamedPhase,
    /**
     * 当前 phase 在本次 phaser 执行中的顺序编号。
     */
    val phaseCount: Int,
    /**
     * 标识 action 是在 phase 主体之前还是之后执行。
     */
    val beforeOrAfter: BeforeOrAfter,
)

/**
 * Phase 前置或后置动作函数。
 *
 * action 可读取 phase 状态、当前数据和编译上下文，通常用于 dump、校验、日志或额外统计。
 */
typealias Action<Data, Context> = (ActionState, Data, Context) -> Unit

/**
 * 顺序组合两个 action，保证左侧 action 先执行、右侧 action 后执行。
 */
infix operator fun <Data, Context> Action<Data, Context>.plus(other: Action<Data, Context>): Action<Data, Context> =
    { phaseState, data, context ->
        this(phaseState, data, context)
        other(phaseState, data, context)
    }
