package org.cangnova.cangjie.phaser

/**
 * 按声明顺序调用通用编译阶段，并将各阶段作为独立命名阶段暴露给 Phaser。
 */
private class CompositePhase<Context : LoggingContext, Input, Output>(
    val phases: List<CompilerPhase<Context, Any?, Any?>>,
) : CompilerPhase<Context, Input, Output> {

    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output {
        @Suppress("UNCHECKED_CAST")
        return phases.fold(input as Any?) { current, phase ->
            phase.invoke(phaseConfig, phaserState, context, current)
        } as Output
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<*, *, *>>> =
        phases.flatMap { phase -> phase.getNamedSubphases(startDepth) }
}

/**
 * 将两个通用编译阶段组合为一个阶段，保留每个命名阶段的完整执行协议。
 */
@Suppress("UNCHECKED_CAST")
public infix fun <Context : LoggingContext, Input, Mid, Output> CompilerPhase<Context, Input, Mid>.then(
    other: CompilerPhase<Context, Mid, Output>,
): CompilerPhase<Context, Input, Output> {
    val left = this as CompilerPhase<Context, Any?, Any?>
    val right = other as CompilerPhase<Context, Any?, Any?>
    val phases = if (this is CompositePhase<Context, *, *>) {
        this.phases + right
    } else {
        listOf(left, right)
    }
    return CompositePhase(phases)
}
