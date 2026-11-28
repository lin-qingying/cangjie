package org.cangnova.cangjie.chir.core.serializer

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirConstantExpression
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirValue
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class OfficialChirV113ChirPackageImporterTest {
    @Test
    fun `imports and round trips official constant expression value identities`() {
        val imported = DefaultOfficialChirV113ChirPackageImporter().importPackage(
            semanticPackage = rawCommonSemanticPackage(),
            packageSemanticId = ChirSemanticId("test:official-v113-constant-round-trip"),
        )
        val importedFunctions = imported.modules.flatMap { it.declarations }
            .filterIsInstance<DefaultChirFunctionDeclaration>()
        val importedConstants = importedFunctions.flatMap { it.blocks }
            .flatMap { it.expressions }.filterIsInstance<ChirConstantExpression>()

        assertTrue(importedConstants.isNotEmpty())
        assertTrue(importedConstants.all { it.resultLocalValue?.semanticId != it.value.semanticId })

        val constantByResultId = importedConstants.associateBy { checkNotNull(it.resultLocalValue).semanticId }
        val use = importedFunctions.asSequence().flatMap { function ->
            function.blocks.asSequence().flatMap { block ->
                block.expressions.filterIsInstance<ChirBinaryExpression>().asSequence().mapNotNull { binary ->
                    val consumedResultId = listOf(binary.left.semanticId, binary.right.semanticId)
                        .firstOrNull(constantByResultId::containsKey)
                        ?: return@mapNotNull null
                    val constant = constantByResultId.getValue(consumedResultId)
                    val constantIndex = block.expressions.indexOf(constant)
                    val binaryIndex = block.expressions.indexOf(binary)
                    if (
                        constantIndex < 0 || binaryIndex <= constantIndex ||
                        (constant.resultType as? ChirResolvedTypeRef)?.type !is ChirPrimitiveType ||
                        (binary.resultType as? ChirResolvedTypeRef)?.type !is ChirPrimitiveType
                    ) {
                        return@mapNotNull null
                    }
                    Triple(block, constant, binary)
                }
            }
        }.firstOrNull() ?: error("official fixture has no primitive constant consumed by a binary expression")
        val (sourceBlock, constant, binary) = use
        val constantResultId = checkNotNull(constant.resultLocalValue).semanticId
        fun retainOnlyConstantResult(value: ChirValue, ordinal: Int): ChirValue = when {
            value.semanticId == constantResultId || value is ChirConstantValue -> value
            else -> ChirConstantValue(
                semanticId = ChirSemanticId("test:official-v113-constant-consumer:other:$ordinal"),
                type = value.type,
                literal = "0",
            )
        }
        val binaryForCodec = binary.copy(
            left = retainOnlyConstantResult(binary.left, 0),
            right = retainOnlyConstantResult(binary.right, 1),
        )
        val functionId = ChirSemanticId("test:official-v113-constant-consumer")
        val blockId = ChirSemanticId("test:official-v113-constant-consumer:block")
        val function = DefaultChirFunctionDeclaration(
            semanticId = functionId,
            name = "constantConsumer",
            returnType = binaryForCodec.resultType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = blockId,
                    name = sourceBlock.name,
                    expressions = listOf(constant, binaryForCodec),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("test:official-v113-constant-consumer:return"),
                        returnValue = checkNotNull(binaryForCodec.resultLocalValue),
                    ),
                ),
            ),
            entryBlockId = blockId,
        )
        val codecInput = ChirPackage(
            semanticId = ChirSemanticId("test:official-v113-constant-consumer-package"),
            name = "officialConstantConsumer",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("test:official-v113-constant-consumer-module"),
                    name = "constantConsumer",
                    declarations = listOf(function),
                ),
            ),
        )
        val restored = ChirPackageCodec.deserialize(ChirPackageCodec.serialize(codecInput))
        val restoredConstants = restored.modules.flatMap { it.declarations }
            .filterIsInstance<DefaultChirFunctionDeclaration>()
            .flatMap { it.blocks }
            .flatMap { it.expressions }
            .filterIsInstance<ChirConstantExpression>()
        val restoredBinary = restored.modules.flatMap { it.declarations }
            .filterIsInstance<DefaultChirFunctionDeclaration>()
            .flatMap { it.blocks }
            .flatMap { it.expressions }
            .filterIsInstance<ChirBinaryExpression>()
            .single()
        val consumedValue = listOf(restoredBinary.left, restoredBinary.right)
            .single { it.semanticId == constant.resultLocalValue?.semanticId }

        assertEquals(1, restoredConstants.size)
        assertTrue(restoredConstants.all { it.resultLocalValue?.semanticId != it.value.semanticId })
        assertInstanceOf(org.cangnova.cangjie.chir.core.value.ChirLocalValue::class.java, consumedValue)
        assertTrue(DefaultChirValidator().validatePackage(imported).issues.isEmpty())
    }

    @Test
    fun `rejects ordinary Func without a body instead of importing it as a declaration`() {
        val source = rawCommonSemanticPackage()
        val function = source.values.first { it.table.schemaType == "Func" }
        val malformedFunction = function.copy(
            table = function.table.copy(
                fields = function.table.fields + (
                    "body" to OfficialChirV113SemanticValue.Reference(
                        OfficialChirV113TableDomain.VALUE,
                        0L,
                    )
                    ),
            ),
        )
        val malformedPackage = source.copyWithValues(
            source.values.map { if (it.id == function.id) malformedFunction else it },
        )

        val error = assertThrows(OfficialChirV113ImportException::class.java) {
            DefaultOfficialChirV113ChirPackageImporter().importPackage(
                semanticPackage = malformedPackage,
                packageSemanticId = ChirSemanticId("test:official-v113-missing-func-body"),
            )
        }

        assertTrue(error.message.orEmpty().contains("declaration-only values must use ImportedFunc"))
    }

    @Test
    fun `rejects ordinary Func with an empty BlockGroup`() {
        val source = rawCommonSemanticPackage()
        val function = source.values.first { it.table.schemaType == "Func" }
        val bodyId = (function.table.fields.getValue("body") as OfficialChirV113SemanticValue.Reference).id.toUInt()
        val body = source.values.single { it.id == bodyId }
        assertTrue(body.table.schemaType == "BlockGroup")
        val emptyBody = body.copy(
            table = body.table.copy(
                fields = body.table.fields + ("blocks" to OfficialChirV113SemanticValue.Sequence(emptyList())),
            ),
        )
        val malformedPackage = source.copyWithValues(
            source.values.map { if (it.id == bodyId) emptyBody else it },
        )

        val error = assertThrows(OfficialChirV113ImportException::class.java) {
            DefaultOfficialChirV113ChirPackageImporter().importPackage(
                semanticPackage = malformedPackage,
                packageSemanticId = ChirSemanticId("test:official-v113-empty-func-body"),
            )
        }

        assertTrue(error.message.orEmpty().contains("empty BlockGroup"))
    }

    private fun rawCommonSemanticPackage(): OfficialChirV113SemanticPackage =
        OfficialChirV113SemanticPackageDecoder.decode(
            Files.readAllBytes(
                repositoryRoot().toPath()
                    .resolve("chir/chir-tree/testData/chirParity/official-generated-chir/cjmpRawCommon/cjmp_raw_chir_v113.chir")
                    .toFile()
                    .also { check(it.isFile) { "missing official RAW CHIR fixture: $it" } }
                    .toPath(),
            ),
        )

    private fun OfficialChirV113SemanticPackage.copyWithValues(
        values: List<OfficialChirV113IndexedTable>,
    ): OfficialChirV113SemanticPackage = OfficialChirV113SemanticPackage(
        metadata = metadata,
        types = types,
        values = values,
        expressions = expressions,
        definitions = definitions,
    )

    private fun repositoryRoot(): File {
        var directory = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            if (File(directory, "settings.gradle.kts").isFile) return directory
            directory = directory.parentFile ?: error("repository root not found")
        }
    }
}
