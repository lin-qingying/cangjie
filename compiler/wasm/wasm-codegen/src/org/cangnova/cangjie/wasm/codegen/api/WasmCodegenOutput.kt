package org.cangnova.cangjie.wasm.codegen.api

import org.cangnova.cangjie.wasm.ir.WasmExport
import org.cangnova.cangjie.wasm.ir.WasmImport
import org.cangnova.cangjie.wasm.ir.WasmGlobalImport
import org.cangnova.cangjie.wasm.common.WasmTarget
import java.nio.file.Path

/** 一个独立 Wasm Core module 的最终产物。 */
data class WasmCodegenOutput(
    /** 生成产物所使用的宿主目标。 */
    val target: WasmTarget,
    /** 模块名称。 */
    val moduleName: String,
    /** 完整 WebAssembly Core 二进制。 */
    val bytes: ByteArray,
    /** 宿主需要提供的函数导入。 */
    val imports: List<WasmImport>,
    /** 宿主需要提供的全局变量导入。 */
    val globalImports: List<WasmGlobalImport>,
    /** 宿主可以使用的导出项。 */
    val exports: List<WasmExport>,
)

/** 将 Wasm 后端结果写成 `.wasm` 文件的公开接口。 */
fun interface WasmArtifactWriter {
    /**
     * 将 module 写入指定路径并返回最终绝对路径。
     *
     * 写出实现必须先写入同目录临时文件，再原子替换目标文件，以免失败时留下截断的 Wasm。
     */
    fun write(output: WasmCodegenOutput, destination: Path): Path
}

/** Default atomic `.wasm` artifact writer. */
class DefaultWasmArtifactWriter : WasmArtifactWriter {
    override fun write(output: WasmCodegenOutput, destination: Path): Path {
        val target = destination.toAbsolutePath().normalize()
        require(target.fileName.toString().endsWith(".wasm", ignoreCase = true)) {
            "WebAssembly artifact path must end in .wasm: $target"
        }
        val parent = requireNotNull(target.parent) { "WebAssembly artifact path must have a parent directory" }
        java.nio.file.Files.createDirectories(parent)
        val temporary = java.nio.file.Files.createTempFile(parent, ".${target.fileName}.", ".tmp")
        try {
            java.nio.channels.FileChannel.open(
                temporary,
                java.nio.file.StandardOpenOption.WRITE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
            ).use { channel ->
                val buffer = java.nio.ByteBuffer.wrap(output.bytes)
                while (buffer.hasRemaining()) channel.write(buffer)
                channel.force(true)
            }
            java.nio.file.Files.move(
                temporary,
                target,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            )
            return target
        } finally {
            java.nio.file.Files.deleteIfExists(temporary)
        }
    }
}
