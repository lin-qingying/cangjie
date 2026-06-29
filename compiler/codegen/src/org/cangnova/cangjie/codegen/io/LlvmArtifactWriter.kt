package org.cangnova.cangjie.codegen.io

import org.cangnova.cangjie.codegen.api.ChirCodegenOutput
import org.cangnova.cangjie.codegen.ir.LlvmModuleArtifact
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * 单个 LLVM module 写出后的文件路径集合。
 */
data class LlvmModuleOutputPath(
    /**
     * LLVM module 名称。
     */
    val moduleName: String,
    /**
     * LLVM IR 文本文件路径。
     */
    val llvmIrPath: Path,
    /**
     * LLVM bitcode 文件路径；未请求写出时为空。
     */
    val bitcodePath: Path?,
    /**
     * 目标 object 文件路径；未请求写出时为空。
     */
    val objectPath: Path?,
)

/**
 * LLVM codegen 产物文件写出器。
 */
class LlvmArtifactWriter {
    /**
     * 写出完整 codegen 输出中的所有 LLVM module。
     */
    fun write(
        output: ChirCodegenOutput,
        outputDirectory: Path,
        emitBitcode: Boolean = true,
        emitObjectCode: Boolean = false,
    ): List<LlvmModuleOutputPath> = writeModules(output.modules, outputDirectory, emitBitcode, emitObjectCode)

    /**
     * 写出给定 LLVM module 列表的 IR、bitcode 与 object code 文件。
     */
    fun writeModules(
        modules: List<LlvmModuleArtifact>,
        outputDirectory: Path,
        emitBitcode: Boolean = true,
        emitObjectCode: Boolean = false,
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

            val objectPath = if (!emitObjectCode) {
                null
            } else {
                val objectCode = requireNotNull(module.objectCode) {
                    "module '${module.name}' does not contain object code bytes; set emitObjectCode=false or enable object emission"
                }
                outputDirectory.resolve("$moduleFileBase.o").also { path ->
                    Files.write(
                        path,
                        objectCode,
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
                objectPath = objectPath,
            )
        }
    }

    /**
     * 将 module 名称规整为可作为文件名的字符串。
     */
    private fun sanitizeForFileName(raw: String): String {
        return raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    /**
     * 规范化 LLVM IR 文本换行，确保文件以单个换行结束。
     */
    private fun normalizeIr(ir: String): String {
        val normalized = ir.replace("\r\n", "\n").trimEnd()
        return "$normalized\n"
    }
}

