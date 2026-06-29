package org.cangnova.cangjie.phaser

import kotlin.system.measureTimeMillis

/**
 * 任意输入、输出和上下文类型的命名 phase 引用。
 */
typealias AnyNamedPhase = NamedCompilerPhase<*, *, *>

/**
 * 命名的编译器 Phase（对齐 K2 的 NamedCompilerPhase）
 */
abstract class NamedCompilerPhase<in Context : LoggingContext, Input, Output>(
    /**
     * Phase 的稳定名称，用于配置匹配、日志输出和 prerequisite 诊断。
     */
    val name: String,
    /**
     * 当前 phase 执行前必须已经完成的命名 phase 集合。
     */
    val prerequisite: Set<NamedCompilerPhase<*, *, *>> = emptySet(),
    /**
     * Phase 主体执行前校验输入数据的条件集合。
     */
    val preconditions: Set<Checker<Input>> = emptySet(),
    /**
     * Phase 主体执行后校验输出数据的条件集合。
     */
    val postconditions: Set<Checker<Output>> = emptySet(),
    /**
     * Phase 主体执行前运行的动作集合，通常用于 dump、日志或状态校验。
     */
    private val preactions: Set<Action<Input, Context>> = emptySet(),
    /**
     * Phase 主体执行后运行的动作集合，可同时读取输入与输出。
     */
    private val postactions: Set<Action<Pair<Input, Output>, Context>> = emptySet(),
    /**
     * 执行该 phase 主体时对 phaser 嵌套深度的增量。
     */
    protected val nlevels: Int = 0,
) : CompilerPhase<Context, Input, Output> {

    /**
     * 按统一流水线协议执行 phase：检查启用状态、前置依赖、action、主体、条件和统计状态。
     */
    final override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output {
        if (!phaseConfig.isEnabled(this)) {
            return outputIfNotEnabled(phaseConfig, phaserState, context, input)
        }

        assert(phaserState.alreadyDone.containsAll(prerequisite)) {
            "Phase $name: phases ${(prerequisite - phaserState.alreadyDone).map { it.name }} are required, but not satisfied"
        }

        context.inVerbosePhase = phaseConfig.isVerbose(this)

        runBefore(phaseConfig, phaserState, context, input)
        val output = if (phaseConfig.needProfiling) {
            runAndProfile(phaseConfig, phaserState, context, input)
        } else {
            phaserState.downlevel(nlevels) {
                phaseBody(context, input)
            }
        }
        runAfter(phaseConfig, phaserState, context, input, output)

        context.inVerbosePhase = false
        phaserState.alreadyDone.add(this)
        phaserState.phaseCount++

        return output
    }

    /**
     * 当前 phase 在启用时真正执行的转换逻辑。
     */
    protected abstract fun phaseBody(context: Context, input: Input): Output

    /**
     * 当前 phase 被配置禁用时应返回的输出。
     */
    protected abstract fun outputIfNotEnabled(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output

    /**
     * 运行 phase 前置 action，并在配置开启时校验输入前置条件。
     */
    private fun runBefore(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.BEFORE)
        for (action in preactions) action(state, input, context)

        if (phaseConfig.checkConditions) {
            for (pre in preconditions) pre(input)
        }
    }

    /**
     * 运行 phase 后置 action，并在配置开启时校验输出后置条件。
     */
    private fun runAfter(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input, output: Output) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.AFTER)
        for (action in postactions) action(state, input to output, context)

        if (phaseConfig.checkConditions) {
            for (post in postconditions) post(output)
        }
    }

    /**
     * 在保留 phase 深度语义的前提下执行主体并输出耗时统计。
     */
    private fun runAndProfile(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, source: Input): Output {
        val result: Output
        val msec = measureTimeMillis {
            result = phaserState.downlevel(nlevels) {
                phaseBody(context, source)
            }
        }
        println("${"\t".repeat(phaserState.depth)}$name: $msec msec")
        return result
    }

    /**
     * 命名 phase 默认只报告自身；组合 phase 可覆盖该方法返回嵌套结构。
     */
    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<*, *, *>>> =
        listOf(startDepth to this)

    /**
     * 返回包含 phase 名称的调试文本。
     */
    override fun toString() = "Compiler Phase @$name"
}
