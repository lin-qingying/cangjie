package org.cangnova.cangjie.cli.common.arguments

import org.cangnova.cangjie.codegen.api.LlvmBackendKind
import org.cangnova.cangjie.codegen.api.ModulePartitionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CodegenArgumentMappingTest {
    @Test
    fun `new pipeline is enabled by default`() {
        val options = CommonCompilerArguments().toCodegenOptions()
        assertTrue(options.enabled)
        assertEquals(LlvmBackendKind.NATIVE_INTEROP, options.llvmBackendKind)
    }

    @Test
    fun `rollback switch routes backend to in-memory`() {
        val options = CommonCompilerArguments(
            rollbackToLegacyCodegenPath = true,
        ).toCodegenOptions()

        assertTrue(options.enabled)
        assertEquals(LlvmBackendKind.IN_MEMORY, options.llvmBackendKind)
    }

    @Test
    fun `legacy enable alias overrides new pipeline toggle`() {
        val options = CommonCompilerArguments(
            enableLlvmBackendPipeline = true,
            enableChirToLlvmCodegen = false,
        ).toCodegenOptions()

        assertFalse(options.enabled)
    }

    @Test
    fun `partition mode aliases map to module partition enum`() {
        val perModule = CommonCompilerArguments(codegenPartitionMode = "per-chir-module")
            .toCodegenOptions()
        val defaultMode = CommonCompilerArguments(codegenPartitionMode = "unknown").toCodegenOptions()

        assertEquals(ModulePartitionMode.PER_CHIR_MODULE, perModule.partitionMode)
        assertEquals(ModulePartitionMode.SINGLE_MODULE, defaultMode.partitionMode)
    }
}
