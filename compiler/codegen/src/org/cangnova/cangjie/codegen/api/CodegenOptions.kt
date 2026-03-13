package org.cangnova.cangjie.codegen.api

data class CodegenOptions(
    val enabled: Boolean = true,
    val partitionMode: ModulePartitionMode = ModulePartitionMode.SINGLE_MODULE,
    val llvmBackendKind: LlvmBackendKind = LlvmBackendKind.NATIVE_INTEROP,
    val nativeInteropTool: String = "cangjie-llvm-interop",
    val nativeInteropFailOnUnavailable: Boolean = false,
    val requiredLlvmMajorVersion: Int = 18,
    val verifyBeforeWrite: Boolean = true,
    val validateChirBeforeLowering: Boolean = true,
    val emitLoweringTrace: Boolean = false,
    val emitBitcode: Boolean = true,
    val emitComments: Boolean = true,
    val emitModuleHeader: Boolean = true,
    val emitRuntimeDeclarations: Boolean = true,
    val targetTriple: String = "unknown-cangjie",
    val targetDataLayout: String = "",
)
