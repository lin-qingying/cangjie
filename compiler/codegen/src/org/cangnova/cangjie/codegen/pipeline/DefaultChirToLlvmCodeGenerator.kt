package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.checker.ChirValidationReportFormatter
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.ChirCodegenOutput
import org.cangnova.cangjie.codegen.api.ChirToLlvmCodeGenerator
import org.cangnova.cangjie.codegen.api.ModulePartitionMode
import org.cangnova.cangjie.codegen.backend.LlvmBackendFactory
import org.cangnova.cangjie.codegen.context.CGContext
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException
import org.cangnova.cangjie.codegen.lowering.ChirLoweringPlan
import org.cangnova.cangjie.codegen.lowering.ChirToLlvmLoweringPipeline
import org.cangnova.cangjie.codegen.module.CGModule

/**
 * 默认 CHIR 到 LLVM codegen 入口。
 */
class DefaultChirToLlvmCodeGenerator : ChirToLlvmCodeGenerator {
    /**
     * CHIR 到 LLVM 之前执行的 lowering pass pipeline。
     */
    private val loweringPipeline = ChirToLlvmLoweringPipeline()
    /**
     * LLVM 后端工厂。
     */
    private val backendFactory = LlvmBackendFactory()

    /**
     * 生成 LLVM 模块产物。
     *
     * 该方法按选项执行 CHIR 校验、module 切分、lowering pipeline、CGModule 降低和后端产物发射。
     */
    override fun generate(input: ChirCodegenInput): ChirCodegenOutput {
        if (!input.options.enabled) {
            return ChirCodegenOutput(emptyList())
        }
        if (input.options.validateChirBeforeLowering) {
            val validationReport = DefaultChirValidator().validatePackage(input.chirPackage)
            if (validationReport.hasErrors) {
                throw CodegenLoweringException(
                    "invalid CHIR package '${input.chirPackage.name}' before lowering:\n${ChirValidationReportFormatter.render(validationReport)}",
                    input.chirPackage.semanticId,
                )
            }
        }
        val backend = backendFactory.createAndInitialize(input.options)
        val plannedModules = when (input.options.partitionMode) {
            ModulePartitionMode.SINGLE_MODULE -> listOf(ModuleLoweringInput(mergeModules(input.chirPackage.modules), emitPackageDefinitions = true))
            ModulePartitionMode.PER_CHIR_MODULE -> buildList {
                if (input.hasPackageDefinitions()) {
                    add(ModuleLoweringInput(input.packageDefinitionsModule(), emitPackageDefinitions = true))
                }
                input.chirPackage.modules.forEach { module ->
                    add(ModuleLoweringInput(module, emitPackageDefinitions = false))
                }
            }
        }
        val loweringResult = loweringPipeline.run(
            initialPlan = ChirLoweringPlan(modules = plannedModules.map { it.module }),
            options = input.options,
        )

        val context = CGContext(
            inputPackage = input.chirPackage,
            options = input.options,
        )

        val plannedById = plannedModules.associateBy { it.module.semanticId }
        val modules = loweringResult.plan.modules.map { module ->
            val planned = plannedById[module.semanticId]
            CGModule(
                context = context,
                module = module,
                backendApi = backend,
                emitPackageDefinitions = planned?.emitPackageDefinitions ?: true,
            ).lower()
        }
        val loweringTrace = if (input.options.emitLoweringTrace) {
            loweringResult.traceLines + "backend=${backend.id}"
        } else {
            loweringResult.traceLines
        }
        return ChirCodegenOutput(modules = modules, loweringTrace = loweringTrace)
    }

    /**
     * 将多个 CHIR module 合并为单个 lowering module。
     */
    private fun mergeModules(modules: List<ChirModule>): ChirModule {
        return ChirModule(
            semanticId = ChirSemanticId("merged:${modules.joinToString("+") { it.semanticId.value }}"),
            name = "merged",
            declarations = modules.flatMap { it.declarations },
        )
    }

    /**
     * 判断输入 package 是否包含需要单独发射的包级定义。
     */
    private fun ChirCodegenInput.hasPackageDefinitions(): Boolean {
        return chirPackage.members.globalFunctions.isNotEmpty() ||
            chirPackage.members.globalVariables.isNotEmpty() ||
            chirPackage.packageInitFunctionId != null ||
            chirPackage.packageLiteralInitFunctionId != null
    }

    /**
     * 为包级定义构造虚拟 CHIR module。
     */
    private fun ChirCodegenInput.packageDefinitionsModule(): ChirModule {
        return ChirModule(
            semanticId = ChirSemanticId("pkg-module:${chirPackage.semanticId.value}"),
            name = "package",
            declarations = emptyList(),
        )
    }

    /**
     * module lowering 前的计划项。
     */
    private data class ModuleLoweringInput(
        /**
         * 待 lowering 的 CHIR module。
         */
        val module: ChirModule,
        /**
         * 是否在该 module 中发射包级定义。
         */
        val emitPackageDefinitions: Boolean,
    )
}
