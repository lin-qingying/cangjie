package org.cangnova.cangjie.llvm.api

/**
 * LLVM 模块 pass 管线描述。
 *
 * `pipelineText` 使用 LLVM PassBuilder 的文本管线语法，例如 `default<O2>`。
 */
data class LlvmPassPipeline(val pipelineText: String) {
    init {
        require(pipelineText.isNotBlank()) { "LLVM pass pipeline must not be blank" }
    }

    companion object {
        /** 创建 LLVM 默认优化管线。 */
        fun defaultOptimization(level: LlvmCodeGenOptimizationLevel): LlvmPassPipeline =
            when (level) {
                LlvmCodeGenOptimizationLevel.NONE -> LlvmPassPipeline("default<O0>")
                LlvmCodeGenOptimizationLevel.LESS -> LlvmPassPipeline("default<O1>")
                LlvmCodeGenOptimizationLevel.DEFAULT -> LlvmPassPipeline("default<O2>")
                LlvmCodeGenOptimizationLevel.AGGRESSIVE -> LlvmPassPipeline("default<O3>")
            }
    }
}

/**
 * LLVM 模块 Pass 管理器。
 *
 * 该类通过 LLVM PassBuilder C API 在模块上执行完整 pass pipeline。
 */
class LlvmPassManager internal constructor(
    private val pipeline: LlvmPassPipeline,
    private val targetMachine: LlvmTargetMachine?,
    private val bindings: LlvmBindings,
) : AutoCloseable {
    /** 在给定模块上运行优化/分析 pass。 */
    fun run(module: LlvmModule) {
        bindings.moduleRunPasses(
            module = module.ref,
            passPipeline = pipeline.pipelineText,
            targetMachine = targetMachine?.ref ?: LlvmTargetMachineRef.NULL,
        )
    }

    override fun close() = Unit
}

/**
 * LLVM Pass 管理器工厂。
 */
object LlvmPassManagers {
    /** 创建模块级 Pass 管理器。 */
    fun createModulePassManager(
        pipeline: LlvmPassPipeline,
        targetMachine: LlvmTargetMachine? = null,
    ): LlvmPassManager =
        LlvmPassManager(
            pipeline = pipeline,
            targetMachine = targetMachine,
            bindings = LlvmBindingRegistry.bindings,
        )
}
