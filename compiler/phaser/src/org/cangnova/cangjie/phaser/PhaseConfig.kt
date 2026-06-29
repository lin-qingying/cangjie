package org.cangnova.cangjie.phaser

/**
 * Phase 配置（对齐 K2 的 PhaseConfig）
 */
class PhaseConfig(
    /**
     * 被显式禁用的 phase 集合。
     */
    private val disabled: PhaseSet = PhaseSet.Empty,
    /**
     * 需要开启 verbose 上下文标记的 phase 集合。
     */
    val verbose: PhaseSet = PhaseSet.Empty,
    /**
     * 需要在执行前 dump 状态的 phase 集合。
     */
    val toDumpStateBefore: PhaseSet = PhaseSet.Empty,
    /**
     * 需要在执行后 dump 状态的 phase 集合。
     */
    val toDumpStateAfter: PhaseSet = PhaseSet.Empty,
    /**
     * 需要在执行前进行状态校验的 phase 集合。
     */
    private val toValidateStateBefore: PhaseSet = PhaseSet.Empty,
    /**
     * 需要在执行后进行状态校验的 phase 集合。
     */
    private val toValidateStateAfter: PhaseSet = PhaseSet.Empty,
    /**
     * dump 输出目录；为空表示不指定文件系统输出位置。
     */
    val dumpToDirectory: String? = null,
    /**
     * dump 时限制输出的全限定名；为空表示不按名称过滤。
     */
    val dumpOnlyFqName: String? = null,
    /**
     * 是否为 phase 主体输出耗时统计。
     */
    val needProfiling: Boolean = false,
    /**
     * 是否执行 phase 输入前置条件和输出后置条件检查。
     */
    val checkConditions: Boolean = false,
) {
    /**
     * 判断 phase 是否未被禁用，可以进入正常执行路径。
     */
    fun isEnabled(phase: AnyNamedPhase): Boolean = phase !in disabled

    /**
     * 判断 phase 是否应设置 verbose 上下文标记。
     */
    fun isVerbose(phase: AnyNamedPhase): Boolean = phase in verbose

    /**
     * 判断 phase 执行前是否需要 dump 状态。
     */
    fun shouldDumpStateBefore(phase: AnyNamedPhase): Boolean = phase in toDumpStateBefore

    /**
     * 判断 phase 执行后是否需要 dump 状态。
     */
    fun shouldDumpStateAfter(phase: AnyNamedPhase): Boolean = phase in toDumpStateAfter

    /**
     * 判断 phase 执行前是否需要进行状态校验。
     */
    fun shouldValidateStateBefore(phase: AnyNamedPhase): Boolean = phase in toValidateStateBefore

    /**
     * 判断 phase 执行后是否需要进行状态校验。
     */
    fun shouldValidateStateAfter(phase: AnyNamedPhase): Boolean = phase in toValidateStateAfter
}
