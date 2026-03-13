package org.cangnova.cangjie.codegen.io

import org.cangnova.cangjie.codegen.api.ChirCodegenOutput
import org.cangnova.cangjie.codegen.ir.LlvmModuleArtifact
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

data class LlvmModuleOutputPath(
    val moduleName: String,
    val llvmIrPath: Path,
    val bitcodePath: Path?,
)

class LlvmArtifactWriter {
    fun write(
        output: ChirCodegenOutput,
        outputDirectory: Path,
        emitBitcode: Boolean = true,
    ): List<LlvmModuleOutputPath> = writeModules(output.modules, outputDirectory, emitBitcode)

    fun writeModules(
        modules: List<LlvmModuleArtifact>,
        outputDirectory: Path,
        emitBitcode: Boolean = true,
    ): List<LlvmModuleOutputPath> {
        Files.createDirectories(outputDirectory)

        return modules.map { module ->
            val moduleFileBase = sanitizeForFileName(module.name.ifBlank { "module" })
            val llvmIrPath = outputDirectory.resolve("$moduleFileBase.ll")
            val normalizedIr = normalizeIr(module.ir)
            Files.writeString(
                llvmIrPath,
                normalizedIr,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )

            val bitcodePath = if (!emitBitcode) {
                null
            } else {
                val bitcode = requireNotNull(module.bitcode) {
                    "module '${module.name}' does not contain bitcode bytes; set emitBitcode=false or enable bitcode emission"
                }
                outputDirectory.resolve("$moduleFileBase.bc").also { path ->
                    Files.write(
                        path,
                        bitcode,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE,
                    )
                }
            }

            LlvmModuleOutputPath(
                moduleName = module.name,
                llvmIrPath = llvmIrPath,
                bitcodePath = bitcodePath,
            )
        }
    }

    private fun sanitizeForFileName(raw: String): String {
        return raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun normalizeIr(ir: String): String {
        val normalized = ir.replace("\r\n", "\n").trimEnd()
        return "$normalized\n"
    }
}

