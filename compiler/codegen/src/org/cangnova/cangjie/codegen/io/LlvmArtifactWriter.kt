package org.cangnova.cangjie.codegen.io

import org.cangnova.cangjie.codegen.api.ChirCodegenOutput
import org.cangnova.cangjie.codegen.ir.LlvmModuleArtifact
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
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
        val plans = modules.map { module ->
            val moduleFileBase = sanitizeForFileName(module.name.ifBlank { "module" })
            val llvmIrPath = outputDirectory.resolve("$moduleFileBase.ll")
            val pendingFiles = mutableListOf(
                PendingArtifactFile(llvmIrPath, normalizeIr(module.ir).toByteArray(StandardCharsets.UTF_8)),
            )

            val bitcodePath = if (!emitBitcode) {
                null
            } else {
                val bitcode = requireNotNull(module.bitcode) {
                    "module '${module.name}' does not contain bitcode bytes; set emitBitcode=false or enable bitcode emission"
                }
                outputDirectory.resolve("$moduleFileBase.bc").also { path ->
                    pendingFiles += PendingArtifactFile(path, bitcode)
                }
            }

            val objectPath = if (!emitObjectCode) {
                null
            } else {
                val objectCode = requireNotNull(module.objectCode) {
                    "module '${module.name}' does not contain object code bytes; set emitObjectCode=false or enable object emission"
                }
                outputDirectory.resolve("$moduleFileBase.o").also { path ->
                    pendingFiles += PendingArtifactFile(path, objectCode)
                }
            }

            ModuleArtifactPlan(
                outputPath = LlvmModuleOutputPath(
                    moduleName = module.name,
                    llvmIrPath = llvmIrPath,
                    bitcodePath = bitcodePath,
                    objectPath = objectPath,
                ),
                pendingFiles = pendingFiles,
            )
        }

        writeTransactionally(plans.flatMap(ModuleArtifactPlan::pendingFiles), outputDirectory)
        return plans.map(ModuleArtifactPlan::outputPath)
    }

    /**
     * 先在输出目录同一文件系统暂存全部工件，再以可回滚的 rename 事务提交。
     * 这样一个 module 的 `.ll` 已写而 `.bc` 写失败时，不会留下混合版本或覆盖旧产物。
     */
    private fun writeTransactionally(files: List<PendingArtifactFile>, outputDirectory: Path) {
        if (files.isEmpty()) return

        val duplicateTargets = files.groupingBy(PendingArtifactFile::target).eachCount()
            .filterValues { count -> count > 1 }
            .keys
        require(duplicateTargets.isEmpty()) {
            "LLVM artifact modules resolve to duplicate output paths: ${duplicateTargets.joinToString()}"
        }

        Files.createDirectories(outputDirectory)
        files.forEach { file ->
            val target = file.target
            require(
                !Files.exists(target, LinkOption.NOFOLLOW_LINKS) ||
                    Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS),
            ) {
                "LLVM artifact output path is not a regular file: $target"
            }
        }

        val transactionDirectory = Files.createTempDirectory(outputDirectory, ".cangjie-llvm-artifacts-")
        val backups = linkedMapOf<Path, Path>()
        val installed = mutableListOf<Path>()
        var failure: Throwable? = null
        var preserveTransactionDirectory = false
        try {
            val stagedFiles = files.mapIndexed { index, file ->
                val staged = transactionDirectory.resolve("new-$index")
                Files.write(
                    staged,
                    file.contents,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE,
                )
                file to staged
            }
            stagedFiles.forEachIndexed { index, (file, staged) ->
                if (Files.exists(file.target, LinkOption.NOFOLLOW_LINKS)) {
                    val backup = transactionDirectory.resolve("old-$index")
                    Files.move(file.target, backup, StandardCopyOption.ATOMIC_MOVE)
                    backups[file.target] = backup
                }
                Files.move(staged, file.target, StandardCopyOption.ATOMIC_MOVE)
                installed.add(file.target)
            }
        } catch (commitFailure: Throwable) {
            failure = commitFailure
            installed.asReversed().forEach { target ->
                runCatching { Files.deleteIfExists(target) }
                    .onFailure { rollbackFailure ->
                        preserveTransactionDirectory = true
                        commitFailure.addSuppressed(rollbackFailure)
                    }
            }
            backups.entries.toList().asReversed().forEach { (target, backup) ->
                runCatching { Files.move(backup, target, StandardCopyOption.ATOMIC_MOVE) }
                    .onFailure { rollbackFailure ->
                        preserveTransactionDirectory = true
                        commitFailure.addSuppressed(rollbackFailure)
                    }
            }
            if (preserveTransactionDirectory) {
                commitFailure.addSuppressed(
                    IllegalStateException("LLVM artifact rollback data preserved at $transactionDirectory"),
                )
            }
            throw commitFailure
        } finally {
            if (!preserveTransactionDirectory) {
                try {
                    Files.list(transactionDirectory).use { children ->
                        children.forEach { child -> Files.deleteIfExists(child) }
                    }
                    Files.deleteIfExists(transactionDirectory)
                } catch (cleanupFailure: Throwable) {
                    val originalFailure = failure
                    if (originalFailure != null) {
                        originalFailure.addSuppressed(cleanupFailure)
                    } else {
                        throw cleanupFailure
                    }
                }
            }
        }
    }

    private data class ModuleArtifactPlan(
        val outputPath: LlvmModuleOutputPath,
        val pendingFiles: List<PendingArtifactFile>,
    )

    private data class PendingArtifactFile(
        val target: Path,
        val contents: ByteArray,
    )

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

