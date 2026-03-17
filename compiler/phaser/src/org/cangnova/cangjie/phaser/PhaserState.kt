package org.cangnova.cangjie.phaser

/**
 * Phaser 状态（对齐 K2 的 PhaserState）
 */
class PhaserState(
    val alreadyDone: MutableSet<AnyNamedPhase> = mutableSetOf(),
    var depth: Int = 0,
    var phaseCount: Int = 0,
) {
    fun copyOf() = PhaserState(alreadyDone.toMutableSet(), depth, phaseCount)
}

inline fun <R> PhaserState.downlevel(nlevels: Int, block: () -> R): R {
    depth += nlevels
    val result = block()
    depth -= nlevels
    return result
}
