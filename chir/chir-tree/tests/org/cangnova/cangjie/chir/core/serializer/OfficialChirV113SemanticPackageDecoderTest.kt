package org.cangnova.cangjie.chir.core.serializer

import org.cangnova.cangjie.chir.core.checker.DefaultChirSourceSemanticGraphValidator
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticDomain
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticGraph
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticNode
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticNodeId
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticTable
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files

class OfficialChirV113SemanticPackageDecoderTest {
    @Test
    fun `imports every official top level domain and nested table`() {
        val bytes = Files.readAllBytes(rawCommonFixture().toPath())
        val wire = OfficialChirV113WirePackageDecoder.decode(bytes)

        val imported = OfficialChirV113SemanticPackageDecoder.decode(bytes)

        assertEquals(wire.metadata, imported.metadata)
        assertEquals(1_258, wire.typeReferences.size)
        assertEquals(2_858, wire.valueReferences.size)
        assertEquals(1_410, wire.expressionReferences.size)
        assertEquals(190, wire.definitionReferences.size)
        assertEquals(5_716, wire.typeReferences.size + wire.valueReferences.size + wire.expressionReferences.size + wire.definitionReferences.size)
        assertEquals(wire.typeReferences.size, imported.types.size)
        assertEquals(wire.valueReferences.size, imported.values.size)
        assertEquals(wire.expressionReferences.size, imported.expressions.size)
        assertEquals(wire.definitionReferences.size, imported.definitions.size)
        assertEquals(
            wire.typeReferences.map { it.id to it.unionTag },
            imported.types.map { it.id to it.unionTag },
        )
        assertEquals(
            wire.valueReferences.map { it.id to it.unionTag },
            imported.values.map { it.id to it.unionTag },
        )
        assertEquals(
            wire.expressionReferences.map { it.id to it.unionTag },
            imported.expressions.map { it.id to it.unionTag },
        )
        assertEquals(
            wire.definitionReferences.map { it.id to it.unionTag },
            imported.definitions.map { it.id to it.unionTag },
        )
        assertEquals(
            imported.types.size + imported.values.size + imported.expressions.size + imported.definitions.size,
            imported.tableCount,
        )
        assertTrue(imported.types.all { it.domain == OfficialChirV113TableDomain.TYPE })
        assertTrue(imported.values.all { it.domain == OfficialChirV113TableDomain.VALUE })
        assertTrue(imported.expressions.all { it.domain == OfficialChirV113TableDomain.EXPRESSION })
        assertTrue(imported.definitions.all { it.domain == OfficialChirV113TableDomain.DEFINITION })
        assertTrue(imported.types.all { it.table.fields.isNotEmpty() })
        assertTrue(imported.values.all { it.table.fields.isNotEmpty() })
        assertTrue(imported.expressions.all { it.table.fields.isNotEmpty() })
        assertTrue(imported.definitions.all { it.table.fields.isNotEmpty() })
        assertTrue(imported.allTables().size > imported.tableCount)
        assertTrue(imported.allReferences().isNotEmpty())
    }

    @Test
    fun `semantic id map keeps every official id domain independent`() {
        val semanticPackage = OfficialChirV113SemanticPackageDecoder.decode(
            Files.readAllBytes(rawCommonFixture().toPath()),
        )
        val ids = OfficialChirV113SemanticIdMap(
            packageSemanticId = ChirSemanticId("test:official-v113-package"),
            semanticPackage = semanticPackage,
        )

        assertEquals(5_716, ids.allIdentities().values.sumOf { it.size })
        assertEquals(
            4,
            OfficialChirV113TableDomain.entries
                .map { domain -> ids.idOf(domain, 1u) }
                .toSet()
                .size,
        )
        OfficialChirV113TableDomain.entries.forEach { domain ->
            assertEquals(
                ids.idOf(domain, 1u),
                ids.idOf(OfficialChirV113SemanticValue.Reference(domain, 1)),
            )
        }
        assertThrows(OfficialChirV113FormatException::class.java) {
            ids.idOf(OfficialChirV113TableDomain.TYPE, 0u)
        }
    }

    @Test
    fun `semantic graph import preserves all domains and validates the complete reference closure`() {
        val semanticPackage = OfficialChirV113SemanticPackageDecoder.decode(
            Files.readAllBytes(rawCommonFixture().toPath()),
        )
        val graph = DefaultOfficialChirV113SemanticGraphImporter().import(
            packageSemanticId = ChirSemanticId("test:official-v113-package"),
            semanticPackage = semanticPackage,
        )

        assertEquals(5_716, graph.nodes.size)
        assertEquals(1_258, graph.roots.getValue(ChirSourceSemanticDomain.TYPE).size)
        assertEquals(2_858, graph.roots.getValue(ChirSourceSemanticDomain.VALUE).size)
        assertEquals(1_410, graph.roots.getValue(ChirSourceSemanticDomain.EXPRESSION).size)
        assertEquals(190, graph.roots.getValue(ChirSourceSemanticDomain.DEFINITION).size)
        assertTrue(graph.nodes.values.any { it.table.fields.isNotEmpty() })
        assertEquals(graph.nodes.size, graph.nodes.keys.map { it.semanticId }.toSet().size)
        assertTrue(DefaultChirSourceSemanticGraphValidator().validate(graph).issues.isEmpty())
    }

    @Test
    fun `semantic graph validator rejects missing cross domain references`() {
        val packageId = ChirSemanticId("test:invalid-source-graph")
        val typeId = ChirSourceSemanticNodeId(
            ChirSourceSemanticDomain.TYPE,
            ChirSemanticId("test:invalid-source-graph:type:1"),
        )
        val missingValueId = ChirSourceSemanticNodeId(
            ChirSourceSemanticDomain.VALUE,
            ChirSemanticId("test:invalid-source-graph:value:missing"),
        )
        val graph = ChirSourceSemanticGraph(
            packageSemanticId = packageId,
            roots = ChirSourceSemanticDomain.entries.associateWith { domain ->
                if (domain == ChirSourceSemanticDomain.TYPE) listOf(typeId) else emptyList()
            },
            nodes = mapOf(
                typeId to ChirSourceSemanticNode(
                    id = typeId,
                    unionTag = 1.toUByte(),
                    table = ChirSourceSemanticTable(
                        schemaType = "Type",
                        fields = mapOf("target" to ChirSourceSemanticValue.Reference(missingValueId)),
                    ),
                ),
            ),
            metadata = emptyMap(),
        )

        val report = DefaultChirSourceSemanticGraphValidator().validate(graph)

        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.message.contains("targets missing VALUE node") })
    }

    @Test
    fun `rejects an invalid nested union table before publishing a package`() {
        val invalid = Files.readAllBytes(rawCommonFixture().toPath())
        overwriteFirstUnionTableOffset(invalid, fieldVtableOffset = 12)

        assertThrows(OfficialChirV113FormatException::class.java) {
            OfficialChirV113SemanticPackageDecoder.decode(invalid)
        }
    }

    @Test
    fun `rejects an out of range package reference`() {
        val invalid = Files.readAllBytes(rawCommonFixture().toPath())
        overwriteRootUIntField(invalid, fieldVtableOffset = 26, value = -1)

        assertThrows(OfficialChirV113FormatException::class.java) {
            OfficialChirV113SemanticPackageDecoder.decode(invalid)
        }
    }

    @Test
    fun `reference domains cover the official schema id comments`() {
        val schemaText = Files.readString(
            repositoryRoot().toPath().resolve("flatbuffers-gen/flatbuffers/PackageFormatV113.fbs"),
        )
        val documented = documentedReferenceFields(schemaText)
        val semantics = OfficialChirV113ReferenceSemantics.documentedFields()

        assertTrue(semantics.keys.containsAll(documented.keys), "missing schema ID semantics: ${documented.keys - semantics.keys}")
        documented.forEach { (field, domain) ->
            assertEquals(domain, semantics[field], "${field.first}.${field.second}")
        }
        assertEquals(
            OfficialChirV113TableDomain.VALUE,
            OfficialChirV113ReferenceSemantics.referenceDomain("MemberVarInfo", "initializerFunc"),
        )
        assertEquals(
            OfficialChirV113TableDomain.VALUE,
            OfficialChirV113ReferenceSemantics.referenceDomain("CustomTypeDef", "varInitializationFunc"),
        )
    }

    private fun OfficialChirV113SemanticPackage.allTables(): List<OfficialChirV113SemanticTable> =
        (types + values + expressions + definitions).flatMap { indexed ->
            buildList { addTableAndNested(indexed.table) }
        }

    private fun OfficialChirV113SemanticPackage.allReferences(): List<OfficialChirV113SemanticValue.Reference> =
        allTables().flatMap { table ->
            table.fields.values.flatMap { value ->
                buildList {
                    fun collect(nested: OfficialChirV113SemanticValue) {
                        when (nested) {
                            is OfficialChirV113SemanticValue.Reference -> add(nested)
                            is OfficialChirV113SemanticValue.Sequence -> nested.values.forEach(::collect)
                            else -> Unit
                        }
                    }
                    collect(value)
                }
            }
        }

    private fun documentedReferenceFields(schemaText: String): Map<Pair<String, String>, OfficialChirV113TableDomain> {
        var tableName: String? = null
        val fields = linkedMapOf<Pair<String, String>, OfficialChirV113TableDomain>()
        val tablePattern = Regex("^table\\s+([A-Za-z0-9_]+)")
        val fieldPattern = Regex("^\\s*([A-Za-z0-9_]+)\\s*:")
        val targetPattern = Regex("\\b(?:use|used)\\s+(types|values|exprs|defs|type|value|def)s?\\b", RegexOption.IGNORE_CASE)
        val identityPattern = Regex("\\bcurrent\\s+id\\s+in\\s+(types|values|exprs|defs)\\b", RegexOption.IGNORE_CASE)
        schemaText.lineSequence().forEach { line ->
            tablePattern.find(line)?.let { tableName = it.groupValues[1] }
            val field = fieldPattern.find(line)?.groupValues?.get(1) ?: return@forEach
            val table = tableName ?: return@forEach
            val comment = line.substringAfter("//", "")
            val domainName = identityPattern.find(comment)?.groupValues?.get(1)
                ?: targetPattern.find(comment)?.groupValues?.get(1)
                ?: if (comment.contains("use valueID", ignoreCase = true)) "values" else null
                ?: return@forEach
            val domain = when (domainName.lowercase()) {
                "types", "type" -> OfficialChirV113TableDomain.TYPE
                "values", "value" -> OfficialChirV113TableDomain.VALUE
                "exprs" -> OfficialChirV113TableDomain.EXPRESSION
                "defs", "def" -> OfficialChirV113TableDomain.DEFINITION
                else -> error("unsupported reference domain $domainName")
            }
            fields[table to field] = domain
        }
        return fields
    }

    private fun overwriteRootUIntField(bytes: ByteArray, fieldVtableOffset: Int, value: Int) {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val rootPosition = buffer.getInt(0)
        val vtablePosition = rootPosition - buffer.getInt(rootPosition)
        val fieldOffset = buffer.getShort(vtablePosition + fieldVtableOffset).toInt() and 0xFFFF
        check(fieldOffset != 0) { "fixture has no root uint32 at vtable offset $fieldVtableOffset" }
        buffer.putInt(rootPosition + fieldOffset, value)
    }

    private fun MutableList<OfficialChirV113SemanticTable>.addTableAndNested(
        table: OfficialChirV113SemanticTable,
    ) {
        add(table)
        table.fields.values.forEach { value ->
            when (value) {
                is OfficialChirV113SemanticValue.Table -> addTableAndNested(value.value)
                is OfficialChirV113SemanticValue.Union -> addTableAndNested(value.value)
                is OfficialChirV113SemanticValue.Sequence -> value.values.forEach { nested ->
                    when (nested) {
                        is OfficialChirV113SemanticValue.Table -> addTableAndNested(nested.value)
                        is OfficialChirV113SemanticValue.Union -> addTableAndNested(nested.value)
                        else -> Unit
                    }
                }
                is OfficialChirV113SemanticValue.Bytes,
                OfficialChirV113SemanticValue.NullValue,
                is OfficialChirV113SemanticValue.Reference,
                is OfficialChirV113SemanticValue.Scalar,
                -> Unit
            }
        }
    }

    private fun overwriteFirstUnionTableOffset(bytes: ByteArray, fieldVtableOffset: Int) {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val rootPosition = buffer.getInt(0)
        val vtablePosition = rootPosition - buffer.getInt(rootPosition)
        val fieldOffset = buffer.getShort(vtablePosition + fieldVtableOffset).toInt() and 0xFFFF
        check(fieldOffset != 0) { "fixture has no union table vector at vtable offset $fieldVtableOffset" }
        val vectorPointerPosition = rootPosition + fieldOffset
        val vectorPosition = vectorPointerPosition + buffer.getInt(vectorPointerPosition)
        check(buffer.getInt(vectorPosition) > 0) { "fixture union vector is empty" }
        buffer.putInt(vectorPosition + Int.SIZE_BYTES, Int.MAX_VALUE)
    }

    private fun rawCommonFixture(): File = repositoryRoot()
        .toPath()
        .resolve("chir/chir-tree/testData/chirParity/official-generated-chir/cjmpRawCommon/cjmp_raw_chir_v113.chir")
        .toFile()
        .also { check(it.isFile) { "missing official raw CHIR fixture: $it" } }

    private fun repositoryRoot(): File {
        var directory = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            if (File(directory, "settings.gradle.kts").isFile) return directory
            directory = directory.parentFile ?: error("repository root not found")
        }
    }
}
