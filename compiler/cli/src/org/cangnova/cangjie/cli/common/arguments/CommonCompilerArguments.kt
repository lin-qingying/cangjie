package org.cangnova.cangjie.cli.common.arguments

data class CommonCompilerArguments(
    var languageVersion: String? = null,
    var languageFeatures: List<String> = emptyList(),
    @Deprecated(
        message = "Use enableLlvmBackendPipeline instead. This alias will be removed after migration.",
    )
    var enableChirToLlvmCodegen: Boolean? = null,
    var enableLlvmBackendPipeline: Boolean = true,
    var rollbackToLegacyCodegenPath: Boolean = false,
    var codegenPartitionMode: String? = null,
    var verifyLlvmModule: Boolean = true,
    var emitLlvmBitcode: Boolean = true,
)
