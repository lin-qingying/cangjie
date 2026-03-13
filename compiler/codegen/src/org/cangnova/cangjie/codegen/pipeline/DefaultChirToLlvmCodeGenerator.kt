package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.ChirCodegenOutput
import org.cangnova.cangjie.codegen.api.ChirToLlvmCodeGenerator
import org.cangnova.cangjie.codegen.api.ModulePartitionMode
import org.cangnova.cangjie.codegen.context.CGContext
import org.cangnova.cangjie.codegen.module.CGModule

class DefaultChirToLlvmCodeGenerator : ChirToLlvmCodeGenerator {
    override fun generate(input: ChirCodegenInput): ChirCodegenOutput {
        if (!input.options.enabled) {
            return ChirCodegenOutput(emptyList())
        }

        val context = CGContext(
            inputPackage = input.chirPackage,
            options = input.options,
        )

        val modules = when (input.options.partitionMode) {
            ModulePartitionMode.SINGLE_MODULE -> listOf(mergeModules(input.chirPackage.modules))
            ModulePartitionMode.PER_CHIR_MODULE -> input.chirPackage.modules
        }.map { CGModule(context, it).lower() }

        return ChirCodegenOutput(modules)
    }

    private fun mergeModules(modules: List<ChirModule>): ChirModule {
        return ChirModule(
            semanticId = org.cangnova.cangjie.chir.core.identity.ChirSemanticId("merged:${modules.joinToString("+") { it.semanticId.value }}"),
            name = "merged",
            declarations = modules.flatMap { it.declarations },
        )
    }
}

