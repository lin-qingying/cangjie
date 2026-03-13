package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.checker.ChirValidationReportFormatter
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
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

class DefaultChirToLlvmCodeGenerator : ChirToLlvmCodeGenerator {
    private val loweringPipeline = ChirToLlvmLoweringPipeline()
    private val backendFactory = LlvmBackendFactory()

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
        val partitionedModules = when (input.options.partitionMode) {
            ModulePartitionMode.SINGLE_MODULE -> listOf(mergeModules(input.chirPackage.modules))
            ModulePartitionMode.PER_CHIR_MODULE -> input.chirPackage.modules
        }
        val loweringResult = loweringPipeline.run(
            initialPlan = ChirLoweringPlan(modules = partitionedModules),
            options = input.options,
        )

        val context = CGContext(
            inputPackage = input.chirPackage,
            options = input.options,
        )

        val modules = loweringResult.plan.modules.map { CGModule(context, it, backendApi = backend).lower() }
        val loweringTrace = if (input.options.emitLoweringTrace) {
            loweringResult.traceLines + "backend=${backend.id}"
        } else {
            loweringResult.traceLines
        }
        return ChirCodegenOutput(modules = modules, loweringTrace = loweringTrace)
    }

    private fun mergeModules(modules: List<ChirModule>): ChirModule {
        return ChirModule(
            semanticId = org.cangnova.cangjie.chir.core.identity.ChirSemanticId("merged:${modules.joinToString("+") { it.semanticId.value }}"),
            name = "merged",
            declarations = modules.flatMap { it.declarations },
        )
    }
}
