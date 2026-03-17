package org.cangnova.cangjie.phaser

import kotlin.system.measureTimeMillis

typealias AnyNamedPhase = NamedCompilerPhase<*, *, *>

/**
 * 命名的编译器 Phase（对齐 K2 的 NamedCompilerPhase）
 */
abstract class NamedCompilerPhase<in Context : LoggingContext, Input, Output>(
    val name: String,
    val prerequisite: Set<NamedCompilerPhase<*, *, *>> = emptySet(),
    val preconditions: Set<Checker<Input>> = emptySet(),
    val postconditions: Set<Checker<Output>> = emptySet(),
    private val preactions: Set<Action<Input, Context>> = emptySet(),
    private val postactions: Set<Action<Pair<Input, Output>, Context>> = emptySet(),
    protected val nlevels: Int = 0
) : CompilerPhase<Context, Input, Output> {

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

    protected abstract fun phaseBody(context: Context, input: Input): Output

    protected abstract fun outputIfNotEnabled(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output

    private fun runBefore(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.BEFORE)
        for (action in preactions) action(state, input, context)

        if (phaseConfig.checkConditions) {
            for (pre in preconditions) pre(input)
        }
    }

    private fun runAfter(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input, output: Output) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.AFTER)
        for (action in postactions) action(state, input to output, context)

        if (phaseConfig.checkConditions) {
            for (post in postconditions) post(output)
        }
    }

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

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<*, *, *>>> =
        listOf(startDepth to this)

    override fun toString() = "Compiler Phase @$name"
}
