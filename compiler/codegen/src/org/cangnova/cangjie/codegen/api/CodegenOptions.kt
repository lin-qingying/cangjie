package org.cangnova.cangjie.codegen.api

data class CodegenOptions(
    val enabled: Boolean = true,
    val partitionMode: ModulePartitionMode = ModulePartitionMode.SINGLE_MODULE,
    val verifyBeforeWrite: Boolean = true,
    val emitBitcode: Boolean = true,
    val emitComments: Boolean = true,
    val emitModuleHeader: Boolean = true,
    val emitRuntimeDeclarations: Boolean = true,
    val targetTriple: String = "unknown-cangjie",
    val targetDataLayout: String = "",
)

