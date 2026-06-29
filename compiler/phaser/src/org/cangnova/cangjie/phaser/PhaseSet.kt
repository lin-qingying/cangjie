package org.cangnova.cangjie.phaser

/**
 * Phase 集合（对齐 K2 的 PhaseSet）
 */
sealed class PhaseSet {
    /**
     * 判断给定 phase 是否属于当前集合。
     */
    abstract operator fun contains(phase: AnyNamedPhase): Boolean

    /**
     * 返回当前集合与另一个集合的并集。
     */
    abstract operator fun plus(phaseSet: PhaseSet): PhaseSet

    /**
     * 按 phase 名称显式枚举的集合，名称匹配忽略大小写。
     */
    class Enum(phases: Set<String>) : PhaseSet() {
        /**
         * 归一化为小写后的 phase 名称集合。
         */
        private val phases: Set<String> = phases.map { it.lowercase() }.toSet()

        /**
         * 判断目标 phase 的名称是否出现在枚举集合中。
         */
        override fun contains(phase: AnyNamedPhase): Boolean =
            phase.name.lowercase() in phases

        /**
         * 合并两个 phase 集合，保持 `All` 和 `Empty` 的代数语义。
         */
        override fun plus(phaseSet: PhaseSet): PhaseSet = when (phaseSet) {
            All -> All
            Empty -> this
            is Enum -> Enum(phases + phaseSet.phases)
        }
    }

    /**
     * 包含所有 phase 的集合。
     */
    object All : PhaseSet() {
        /**
         * 任意 phase 都属于全集。
         */
        override fun contains(phase: AnyNamedPhase): Boolean = true

        /**
         * 全集与任何集合的并集仍为全集。
         */
        override fun plus(phaseSet: PhaseSet): PhaseSet = All
    }

    /**
     * 不包含任何 phase 的集合。
     */
    object Empty : PhaseSet() {
        /**
         * 任意 phase 都不属于空集。
         */
        override fun contains(phase: AnyNamedPhase): Boolean = false

        /**
         * 空集与其他集合的并集等于另一个集合。
         */
        override fun plus(phaseSet: PhaseSet): PhaseSet = phaseSet
    }
}
