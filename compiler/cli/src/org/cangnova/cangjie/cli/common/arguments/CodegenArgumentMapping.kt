package org.cangnova.cangjie.cli.common.arguments

import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.api.LlvmBackendKind
import org.cangnova.cangjie.codegen.api.ModulePartitionMode

fun CommonCompilerArguments.toCodegenOptions(): CodegenOptions {
    val legacyEnable = enableChirToLlvmCodegen
    val enableNewPipeline = legacyEnable ?: enableLlvmBackendPipeline
    return CodegenOptions(
        enabled = enableNewPipeline,
        partitionMode = codegenPartitionMode.toModulePartitionMode(),
        verifyBeforeWrite = verifyLlvmModule,
        emitBitcode = emitLlvmBitcode,
        llvmBackendKind = if (rollbackToLegacyCodegenPath) LlvmBackendKind.IN_MEMORY else LlvmBackendKind.NATIVE_INTEROP,
    )
}

private fun String?.toModulePartitionMode(): ModulePartitionMode {
    return when (this?.trim()?.lowercase()) {
        null,
        "",
        "single",
        "single_module",
        "single-module",
        "merged",
        -> ModulePartitionMode.SINGLE_MODULE

        "per",
        "per_chir_module",
        "per-chir-module",
        "per_module",
        "per-module",
        -> ModulePartitionMode.PER_CHIR_MODULE

        else -> ModulePartitionMode.SINGLE_MODULE
    }
}
