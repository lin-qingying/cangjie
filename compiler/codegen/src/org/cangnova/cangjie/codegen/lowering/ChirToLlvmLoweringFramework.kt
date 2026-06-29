package org.cangnova.cangjie.codegen.lowering

import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.validateMinimalControlFlow
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException

/**
 * CHIR 到 LLVM lowering 流水线中逐 pass 传递的计划。
 */
data class ChirLoweringPlan(
    /**
     * 待 lower 的 CHIR module 列表。
     */
    val modules: List<ChirModule>,
    /**
     * 每个 module 的符号索引。
     */
    val symbolIndex: Map<String, Set<String>> = emptyMap(),
    /**
     * 每个 module 的类型索引。
     */
    val typeIndex: Map<String, Set<String>> = emptyMap(),
)

/**
 * 单个 lowering pass 或完整 pipeline 的执行结果。
 */
data class ChirLoweringPassResult(
    /**
     * pass 处理后的 lowering 计划。
     */
    val plan: ChirLoweringPlan,
    /**
     * pass 产生的可选 trace 行。
     */
    val traceLines: List<String> = emptyList(),
)

/**
 * CHIR 到 LLVM lowering pass 的统一接口。
 */
interface ChirToLlvmLoweringPass {
    /**
     * lowering pass 标识。
     */
    val id: String
        get() = this::class.simpleName ?: "unnamed-pass"

    /**
     * 执行当前 lowering pass。
     */
    fun run(plan: ChirLoweringPlan, options: CodegenOptions): ChirLoweringPassResult
}

/**
 * 收集 module 声明符号的 lowering pass。
 */
class SymbolCollectionLoweringPass : ChirToLlvmLoweringPass {
    /**
     * pass 标识。
     */
    override val id: String = "symbol-collection"

    /**
     * 为每个 module 建立声明 semanticId 索引。
     */
    override fun run(plan: ChirLoweringPlan, options: CodegenOptions): ChirLoweringPassResult {
        val symbolIndex = plan.modules.associate { module ->
            module.name to module.declarations.map { it.semanticId.value }.toSet()
        }
        val trace = if (options.emitLoweringTrace) {
            listOf("[$id] collected symbols for ${symbolIndex.size} module(s)")
        } else {
            emptyList()
        }
        return ChirLoweringPassResult(plan.copy(symbolIndex = symbolIndex), trace)
    }
}

/**
 * 收集函数返回类型映射信息的 lowering pass。
 */
class TypeMappingLoweringPass : ChirToLlvmLoweringPass {
    /**
     * pass 标识。
     */
    override val id: String = "type-mapping"

    /**
     * 为每个 module 建立函数返回类型索引。
     */
    override fun run(plan: ChirLoweringPlan, options: CodegenOptions): ChirLoweringPassResult {
        val typeIndex = plan.modules.associate { module ->
            module.name to module.declarations
                .filterIsInstance<ChirFunctionDeclaration>()
                .map { it.returnType.renderName }
                .toSet()
        }
        val trace = if (options.emitLoweringTrace) {
            listOf("[$id] mapped return types for ${typeIndex.size} module(s)")
        } else {
            emptyList()
        }
        return ChirLoweringPassResult(plan.copy(typeIndex = typeIndex), trace)
    }
}

/**
 * 校验 CHIR 控制流最小契约的 lowering pass。
 */
class ControlFlowContractLoweringPass : ChirToLlvmLoweringPass {
    /**
     * pass 标识。
     */
    override val id: String = "control-flow-contract"

    /**
     * 检查每个函数的基本块、入口块和终结符结构。
     */
    override fun run(plan: ChirLoweringPlan, options: CodegenOptions): ChirLoweringPassResult {
        val trace = mutableListOf<String>()
        plan.modules.forEach { module ->
            module.declarations.filterIsInstance<ChirFunctionDeclaration>().forEach { function ->
                val errors = validateMinimalControlFlow(function)
                if (errors.isNotEmpty()) {
                    throw CodegenLoweringException(
                        "function ${function.name} violates control-flow contract: ${errors.joinToString("; ")}",
                        function.semanticId,
                    )
                }
            }
            if (options.emitLoweringTrace) {
                trace += "[$id] validated control-flow for module ${module.name}"
            }
        }
        return ChirLoweringPassResult(plan, trace)
    }
}

/**
 * CHIR 到 LLVM lowering pass pipeline。
 */
class ChirToLlvmLoweringPipeline(
    /**
     * 依次执行的 lowering pass 列表。
     */
    private val passes: List<ChirToLlvmLoweringPass> = listOf(
        SymbolCollectionLoweringPass(),
        TypeMappingLoweringPass(),
        ControlFlowContractLoweringPass(),
    ),
) {
    /**
     * 从初始 lowering 计划开始顺序执行所有 pass。
     */
    fun run(initialPlan: ChirLoweringPlan, options: CodegenOptions): ChirLoweringPassResult {
        var current = initialPlan
        val trace = mutableListOf<String>()
        passes.forEach { pass ->
            val result = pass.run(current, options)
            current = result.plan
            trace += result.traceLines
        }
        return ChirLoweringPassResult(current, trace)
    }
}
