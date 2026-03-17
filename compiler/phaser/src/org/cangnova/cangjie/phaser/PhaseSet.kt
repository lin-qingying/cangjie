package org.cangnova.cangjie.phaser

/**
 * Phase 集合（对齐 K2 的 PhaseSet）
 */
sealed class PhaseSet {
    abstract operator fun contains(phase: AnyNamedPhase): Boolean
    abstract operator fun plus(phaseSet: PhaseSet): PhaseSet

    class Enum(phases: Set<String>) : PhaseSet() {
        private val phases: Set<String> = phases.map { it.lowercase() }.toSet()

        override fun contains(phase: AnyNamedPhase): Boolean =
            phase.name.lowercase() in phases

        override fun plus(phaseSet: PhaseSet): PhaseSet = when (phaseSet) {
            All -> All
            Empty -> this
            is Enum -> Enum(phases + phaseSet.phases)
        }
    }

    object All : PhaseSet() {
        override fun contains(phase: AnyNamedPhase): Boolean = true
        override fun plus(phaseSet: PhaseSet): PhaseSet = All
    }

    object Empty : PhaseSet() {
        override fun contains(phase: AnyNamedPhase): Boolean = false
        override fun plus(phaseSet: PhaseSet): PhaseSet = phaseSet
    }
}
