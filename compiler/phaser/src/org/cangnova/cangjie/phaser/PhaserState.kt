package org.cangnova.cangjie.phaser

/**
 * Phaser 状态（对齐 K2 的 PhaserState）
 */
class PhaserState(
    /**
     * 当前流水线中已经成功执行完毕的命名 phase 集合。
     */
    val alreadyDone: MutableSet<AnyNamedPhase> = mutableSetOf(),
    /**
     * 当前 phase 嵌套深度，用于 profiling 和日志缩进。
     */
    var depth: Int = 0,
    /**
     * 已执行 phase 的顺序计数。
     */
    var phaseCount: Int = 0,
) {
    /**
     * 创建可继续独立推进的 phaser 状态副本。
     */
    fun copyOf() = PhaserState(alreadyDone.toMutableSet(), depth, phaseCount)
}

/**
 * 在指定嵌套深度增量下执行代码块，并在退出时恢复原始深度。
 */
inline fun <R> PhaserState.downlevel(nlevels: Int, block: () -> R): R {
    depth += nlevels
    val result = block()
    depth -= nlevels
    return result
}
