package org.cangnova.cangjie.phaser

/**
 * 编译器 Phase 基础接口（对齐 K2 的 CompilerPhase）
 */
interface CompilerPhase<in Context : LoggingContext, Input, Output> {
    /**
     * 在给定配置、phaser 状态和编译上下文中执行当前 phase。
     */
    fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output

    /**
     * 返回当前 phase 以及嵌套子 phase 的名称和层级，用于日志、dump 和诊断展示。
     */
    fun getNamedSubphases(startDepth: Int = 0): List<Pair<Int, NamedCompilerPhase<*, *, *>>> = emptyList()
}

/**
 * 以顶层 phase 方式启动执行，自动创建新的 `PhaserState`。
 */
fun <Context : LoggingContext, Input, Output> CompilerPhase<Context, Input, Output>.invokeToplevel(
    phaseConfig: PhaseConfig,
    context: Context,
    input: Input
): Output = invoke(phaseConfig, PhaserState(), context, input)

/**
 * Phase 输入或输出状态检查函数。
 */
typealias Checker<Data> = (Data) -> Unit
