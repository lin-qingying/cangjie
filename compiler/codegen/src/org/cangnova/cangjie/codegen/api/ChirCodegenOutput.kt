package org.cangnova.cangjie.codegen.api

import org.cangnova.cangjie.codegen.io.LlvmArtifactWriter
import org.cangnova.cangjie.codegen.io.LlvmModuleOutputPath
import org.cangnova.cangjie.codegen.ir.LlvmModuleArtifact
import java.nio.file.Path

/**
 * CHIR 到 LLVM codegen 的输出模型。
 */
data class ChirCodegenOutput(
    /**
     * 生成出的 LLVM 模块产物列表。
     */
    val modules: List<LlvmModuleArtifact>,
    /**
     * 可选的 lowering 流水线追踪信息。
     */
    val loweringTrace: List<String> = emptyList(),
)

/**
 * 将 codegen 输出写入文件系统中的 LLVM IR、bitcode 或 object code 文件。
 */
fun ChirCodegenOutput.writeLlvmArtifacts(
    outputDirectory: Path,
    emitBitcode: Boolean = true,
    emitObjectCode: Boolean = false,
    writer: LlvmArtifactWriter = LlvmArtifactWriter(),
): List<LlvmModuleOutputPath> = writer.write(this, outputDirectory, emitBitcode, emitObjectCode)

/**
 * CHIR 到 LLVM 后端生成器的统一入口。
 */
fun interface ChirToLlvmCodeGenerator {
    /**
     * 根据给定 CHIR 输入执行 LLVM codegen，并返回内存中的模块产物。
     */
    fun generate(input: ChirCodegenInput): ChirCodegenOutput
}
