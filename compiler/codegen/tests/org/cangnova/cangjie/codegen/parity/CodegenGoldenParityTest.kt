package org.cangnova.cangjie.codegen.parity

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.pipeline.DefaultChirToLlvmCodeGenerator
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

@Tag("parity")
class CodegenGoldenParityTest {
    private val comparator = LlvmIrParityComparator()

    @Test
    fun `simple-return matches cpp baseline`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:main"),
            name = "main",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:zero"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val generated = DefaultChirToLlvmCodeGenerator()
            .generate(
                ChirCodegenInput(
                    chirPackage = ChirPackage(
                        semanticId = ChirSemanticId("pkg:simple"),
                        name = "simple",
                        modules = listOf(
                            ChirModule(
                                semanticId = ChirSemanticId("mod:simple"),
                                name = "simple",
                                declarations = listOf(function),
                            ),
                        ),
                    ),
                    options = CodegenOptions(
                        enabled = true,
                        verifyBeforeWrite = true,
                        emitBitcode = false,
                        emitComments = false,
                        emitModuleHeader = false,
                        emitRuntimeDeclarations = false,
                    ),
                ),
            )
            .modules
            .single()
            .ir

        val expected = loadBaselineText("simple-return.llvmir.txt")
        val result = comparator.compare(expected, generated)
        assertTrue(result.matches, comparator.formatFirstDiffReport(result))
    }

    private fun loadBaselineText(fileName: String): String {
        val fileCandidates = listOf(
            Path.of("testResources", "chir-parity", "cpp-baseline", fileName),
            Path.of("compiler", "codegen", "testResources", "chir-parity", "cpp-baseline", fileName),
        )
        fileCandidates.firstOrNull { Files.exists(it) }?.let { path ->
            return Files.readString(path, StandardCharsets.UTF_8)
        }

        val classLoader = Thread.currentThread().contextClassLoader
        val resourceCandidates = listOf(
            "chir-parity/cpp-baseline/$fileName",
            "cpp-baseline/$fileName",
        )
        val stream = resourceCandidates
            .asSequence()
            .mapNotNull { classLoader.getResourceAsStream(it) }
            .firstOrNull()
            ?: error("missing baseline resource for $fileName")

        val text = stream.bufferedReader().use { it.readText() }
        check(text.isNotBlank()) { "baseline resource for $fileName is empty" }
        return text
    }
}
