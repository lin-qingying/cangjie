package org.cangnova.cangjie.phaser

/**
 * Phase 配置（对齐 K2 的 PhaseConfig）
 */
class PhaseConfig(
    private val disabled: PhaseSet = PhaseSet.Empty,
    val verbose: PhaseSet = PhaseSet.Empty,
    val toDumpStateBefore: PhaseSet = PhaseSet.Empty,
    val toDumpStateAfter: PhaseSet = PhaseSet.Empty,
    private val toValidateStateBefore: PhaseSet = PhaseSet.Empty,
    private val toValidateStateAfter: PhaseSet = PhaseSet.Empty,
    val dumpToDirectory: String? = null,
    val dumpOnlyFqName: String? = null,
    val needProfiling: Boolean = false,
    val checkConditions: Boolean = false,
) {
    fun isEnabled(phase: AnyNamedPhase): Boolean = phase !in disabled
    fun isVerbose(phase: AnyNamedPhase): Boolean = phase in verbose
    fun shouldDumpStateBefore(phase: AnyNamedPhase): Boolean = phase in toDumpStateBefore
    fun shouldDumpStateAfter(phase: AnyNamedPhase): Boolean = phase in toDumpStateAfter
    fun shouldValidateStateBefore(phase: AnyNamedPhase): Boolean = phase in toValidateStateBefore
    fun shouldValidateStateAfter(phase: AnyNamedPhase): Boolean = phase in toValidateStateAfter
}
