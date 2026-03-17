package org.cangnova.cangjie.phaser

/**
 * 编译器 Phase 基础接口（对齐 K2 的 CompilerPhase）
 */
interface CompilerPhase<in Context : LoggingContext, Input, Output> {
    fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output

    fun getNamedSubphases(startDepth: Int = 0): List<Pair<Int, NamedCompilerPhase<*, *, *>>> = emptyList()
}

fun <Context : LoggingContext, Input, Output> CompilerPhase<Context, Input, Output>.invokeToplevel(
    phaseConfig: PhaseConfig,
    context: Context,
    input: Input
): Output = invoke(phaseConfig, PhaserState(), context, input)

typealias Checker<Data> = (Data) -> Unit
