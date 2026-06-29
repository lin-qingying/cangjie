package org.cangnova.cangjie.codegen.io

import org.cangnova.cangjie.codegen.api.ChirCodegenOutput
import org.cangnova.cangjie.codegen.api.writeLlvmArtifacts
import org.cangnova.cangjie.codegen.ir.LlvmModuleArtifact
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * `LlvmArtifactWriter` 的文件写出契约测试。
 */
class LlvmArtifactWriterTest {
    /**
     * 验证 LLVM IR、bitcode 和 object code 文件都会按请求写出。
     */
    @Test
    fun `writes llvm ir and bitcode files`() {
        val tempDir = Files.createTempDirectory("llvm-writer")
        val output = ChirCodegenOutput(
            modules = listOf(
                LlvmModuleArtifact(
                    name = "sample-module",
                    ir = "define i32 @main() {\n  ret i32 0\n}",
                    functions = emptyList(),
                    bitcode = byteArrayOf(0x42, 0x43),
                    objectCode = byteArrayOf(0x7F, 0x45),
                ),
            ),
        )

        val written = output.writeLlvmArtifacts(tempDir, emitBitcode = true, emitObjectCode = true)

        assertEquals(1, written.size)
        val paths = written.single()
        assertEquals("sample-module", paths.moduleName)
        assertEquals(
            "define i32 @main() {\n  ret i32 0\n}\n",
            Files.readString(paths.llvmIrPath, StandardCharsets.UTF_8),
        )
        assertArrayEquals(byteArrayOf(0x42, 0x43), Files.readAllBytes(paths.bitcodePath))
        assertArrayEquals(byteArrayOf(0x7F, 0x45), Files.readAllBytes(paths.objectPath))
    }

    /**
     * 验证 module 名称会被规整为安全文件名。
     */
    @Test
    fun `sanitizes module name for output path`() {
        val tempDir = Files.createTempDirectory("llvm-writer")
        val output = ChirCodegenOutput(
            modules = listOf(
                LlvmModuleArtifact(
                    name = "mod:with space",
                    ir = "define void @f() { ret void }",
                    functions = emptyList(),
                    bitcode = byteArrayOf(),
                ),
            ),
        )

        val written = output.writeLlvmArtifacts(tempDir, emitBitcode = false)
        val llvmIrPath = written.single().llvmIrPath
        assertEquals("mod_with_space.ll", llvmIrPath.fileName.toString())
    }

    /**
     * 验证请求 bitcode 但 module 没有 bitcode 字节时会失败。
     */
    @Test
    fun `fails when bitcode requested but module has none`() {
        val tempDir = Files.createTempDirectory("llvm-writer")
        val output = ChirCodegenOutput(
            modules = listOf(
                LlvmModuleArtifact(
                    name = "missing-bitcode",
                    ir = "define void @f() { ret void }",
                    functions = emptyList(),
                    bitcode = null,
                ),
            ),
        )

        assertThrows<IllegalArgumentException> {
            output.writeLlvmArtifacts(tempDir, emitBitcode = true)
        }
    }

    /**
     * 验证请求 object code 但 module 没有 object 字节时会失败。
     */
    @Test
    fun `fails when object code requested but module has none`() {
        val tempDir = Files.createTempDirectory("llvm-writer")
        val output = ChirCodegenOutput(
            modules = listOf(
                LlvmModuleArtifact(
                    name = "missing-object",
                    ir = "define void @f() { ret void }",
                    functions = emptyList(),
                    bitcode = null,
                    objectCode = null,
                ),
            ),
        )

        assertThrows<IllegalArgumentException> {
            output.writeLlvmArtifacts(tempDir, emitBitcode = false, emitObjectCode = true)
        }
    }
}

