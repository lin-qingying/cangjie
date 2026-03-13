package org.cangnova.cangjie.codegen.lowering

import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.validateMinimalControlFlow
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException

data class ChirLoweringPlan(
    val modules: List<ChirModule>,
    val symbolIndex: Map<String, Set<String>> = emptyMap(),
    val typeIndex: Map<String, Set<String>> = emptyMap(),
)

data class ChirLoweringPassResult(
    val plan: ChirLoweringPlan,
    val traceLines: List<String> = emptyList(),
)

interface ChirToLlvmLoweringPass {
    val id: String
        get() = this::class.simpleName ?: "unnamed-pass"

    fun run(plan: ChirLoweringPlan, options: CodegenOptions): ChirLoweringPassResult
}

class SymbolCollectionLoweringPass : ChirToLlvmLoweringPass {
    override val id: String = "symbol-collection"

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

class TypeMappingLoweringPass : ChirToLlvmLoweringPass {
    override val id: String = "type-mapping"

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

class ControlFlowContractLoweringPass : ChirToLlvmLoweringPass {
    override val id: String = "control-flow-contract"

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

class ChirToLlvmLoweringPipeline(
    private val passes: List<ChirToLlvmLoweringPass> = listOf(
        SymbolCollectionLoweringPass(),
        TypeMappingLoweringPass(),
        ControlFlowContractLoweringPass(),
    ),
) {
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
