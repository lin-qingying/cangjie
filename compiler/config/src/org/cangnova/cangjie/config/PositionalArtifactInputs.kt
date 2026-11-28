package org.cangnova.cangjie.config

import java.nio.file.Path

/**
 * 命令行位置参数中由非源码阶段消费的工件种类。
 */
enum class PositionalArtifactKind {
    OBJECT,
    ARCHIVE,
    COFF_OBJECT,
    LLVM_BITCODE,
    CJO,
}

/**
 * 经 CLI 验证的位置工件，同时保留用户输入和驱动解析出的路径。
 */
data class PositionalArtifactInput(
    val kind: PositionalArtifactKind,
    val originalPath: String,
    val resolvedPath: Path,
)

/**
 * 位置工件依命令行顺序保存，供链接器、CJO 扫描器和后端分别消费。
 */
var CompilerConfiguration.positionalArtifactInputs: List<PositionalArtifactInput>
    get() = get(CLIConfigurationKeys.POSITIONAL_ARTIFACT_INPUTS).orEmpty()
    set(value) {
        put(CLIConfigurationKeys.POSITIONAL_ARTIFACT_INPUTS, value.toList())
    }
