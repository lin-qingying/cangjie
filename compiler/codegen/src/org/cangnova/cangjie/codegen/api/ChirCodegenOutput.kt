package org.cangnova.cangjie.codegen.api

import org.cangnova.cangjie.codegen.io.LlvmArtifactWriter
import org.cangnova.cangjie.codegen.io.LlvmModuleOutputPath
import org.cangnova.cangjie.codegen.ir.LlvmModuleArtifact
import java.nio.file.Path

data class ChirCodegenOutput(
    val modules: List<LlvmModuleArtifact>,
    val loweringTrace: List<String> = emptyList(),
)

fun ChirCodegenOutput.writeLlvmArtifacts(
    outputDirectory: Path,
    emitBitcode: Boolean = true,
    emitObjectCode: Boolean = false,
    writer: LlvmArtifactWriter = LlvmArtifactWriter(),
): List<LlvmModuleOutputPath> = writer.write(this, outputDirectory, emitBitcode, emitObjectCode)

fun interface ChirToLlvmCodeGenerator {
    fun generate(input: ChirCodegenInput): ChirCodegenOutput
}
