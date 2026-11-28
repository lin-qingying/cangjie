package org.cangnova.cangjie.chir.core.serializer

import PackageFormatV113.CHIRPackage
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files

class OfficialChirV113WirePackageDecoderTest {
    @Test
    fun `official v1_1_3 common compilation emits a raw package`() {
        val fixture = rawCommonFixture()
        val bytes = Files.readAllBytes(fixture.toPath())

        val packageData = OfficialChirV113WirePackageDecoder.decode(bytes)

        assertEquals("cjmp_raw_chir_v113", packageData.metadata.name)
        assertEquals(OfficialChirV113Phase.RAW, packageData.metadata.phase)
        assertTrue(packageData.typeReferences.isNotEmpty())
        assertTrue(packageData.valueReferences.isNotEmpty())
        assertTrue(packageData.expressionReferences.isNotEmpty())
        assertTrue(packageData.definitionReferences.isNotEmpty())
        assertArrayEquals(bytes, packageData.toByteArray())
        assertTrue(File(fixture.parentFile, "cjmp_raw_chir_v113.cjo").isFile)
    }

    @Test
    fun `official package metadata and typed table references are preserved`() {
        val fixture = rawCommonFixture()
        val bytes = Files.readAllBytes(fixture.toPath())
        val root = CHIRPackage.getRootAsCHIRPackage(ByteBuffer.wrap(bytes))

        val packageData = OfficialChirV113WirePackageDecoder.decode(bytes)

        assertEquals(root.name, packageData.metadata.name)
        assertEquals(root.path, packageData.metadata.path)
        assertEquals(root.pkgAccessLevel, packageData.metadata.accessLevel.wireValue)
        assertEquals(root.phase, packageData.metadata.phase.wireValue)
        assertEquals(root.packageInitFunc, packageData.metadata.packageInitFunctionId)
        assertEquals(root.packageLiteralInitFunc, packageData.metadata.packageLiteralInitFunctionId)
        assertEquals(root.maxImportedValueId, packageData.metadata.maxImportedValueId)
        assertEquals(root.maxImportedStructId, packageData.metadata.maxImportedStructId)
        assertEquals(root.maxImportedClassId, packageData.metadata.maxImportedClassId)
        assertEquals(root.maxImportedEnumId, packageData.metadata.maxImportedEnumId)
        assertEquals(root.maxImportedExtendId, packageData.metadata.maxImportedExtendId)
        assertEquals(OfficialChirV113Phase.RAW, packageData.metadata.phase)
        assertTrue(!packageData.metadata.name.isNullOrBlank())
        assertTrue(packageData.metadata.path.isNullOrBlank())
        assertTrue(packageData.typeReferences.isNotEmpty())
        assertTrue(packageData.valueReferences.isNotEmpty())
        assertTrue(packageData.expressionReferences.isNotEmpty())
        assertTrue(packageData.definitionReferences.isNotEmpty())
        assertEquals(
            packageData.typeReferences.indices.map { it.toUInt() + 1u },
            packageData.typeReferences.map { it.id },
        )
        assertTrue(packageData.typeReferences.all { it.domain == OfficialChirV113TableDomain.TYPE })
        assertTrue(packageData.valueReferences.all { it.domain == OfficialChirV113TableDomain.VALUE })
        assertTrue(packageData.expressionReferences.all { it.domain == OfficialChirV113TableDomain.EXPRESSION })
        assertTrue(packageData.definitionReferences.all { it.domain == OfficialChirV113TableDomain.DEFINITION })
        assertEquals(
            (0 until root.typesTypeLength).map(root::typesType),
            packageData.typeReferences.map { it.unionTag },
        )
        assertEquals(
            (0 until root.valuesTypeLength).map(root::valuesType),
            packageData.valueReferences.map { it.unionTag },
        )
        assertEquals(
            (0 until root.exprsTypeLength).map(root::exprsType),
            packageData.expressionReferences.map { it.unionTag },
        )
        assertEquals(
            (0 until root.defsTypeLength).map(root::defsType),
            packageData.definitionReferences.map { it.unionTag },
        )
        assertArrayEquals(bytes, packageData.toByteArray())
    }

    @Test
    fun `decoded package owns its input bytes`() {
        val original = Files.readAllBytes(rawCommonFixture().toPath())
        val packageData = OfficialChirV113WirePackageDecoder.decode(original)
        original.fill(0)

        assertTrue(packageData.metadata.name.isNullOrBlank().not())
        assertTrue(packageData.toByteArray().any { it != 0.toByte() })
    }

    @Test
    fun `payload without a valid flatbuffers root table is rejected`() {
        val invalid = ByteArray(16)

        assertThrows(OfficialChirV113FormatException::class.java) {
            OfficialChirV113WirePackageDecoder.decode(invalid)
        }
    }

    @Test
    fun `root union vector cannot allocate references beyond the payload`() {
        val invalid = Files.readAllBytes(rawCommonFixture().toPath())
        overwriteRootVectorLength(invalid, fieldVtableOffset = 12, length = Int.MAX_VALUE)

        assertThrows(OfficialChirV113FormatException::class.java) {
            OfficialChirV113WirePackageDecoder.decode(invalid)
        }
    }

    private fun overwriteRootVectorLength(bytes: ByteArray, fieldVtableOffset: Int, length: Int) {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val rootPosition = buffer.getInt(0)
        val vtablePosition = rootPosition - buffer.getInt(rootPosition)
        val fieldOffset = buffer.getShort(vtablePosition + fieldVtableOffset).toInt()
        check(fieldOffset != 0) { "fixture does not contain root vector at vtable offset $fieldVtableOffset" }
        val vectorPointerPosition = rootPosition + fieldOffset
        val vectorPosition = vectorPointerPosition + buffer.getInt(vectorPointerPosition)
        buffer.putInt(vectorPosition, length)
    }

    private fun rawCommonFixture(): File {
        return repositoryRoot()
            .toPath()
            .resolve("chir/chir-tree/testData/chirParity/official-generated-chir/cjmpRawCommon/cjmp_raw_chir_v113.chir")
            .toFile()
            .also { check(it.isFile) { "missing official raw CJMP CHIR fixture: $it" } }
    }

    private fun repositoryRoot(): File {
        var directory = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            if (File(directory, "settings.gradle.kts").isFile) {
                return directory
            }
            directory = directory.parentFile ?: error("repository root not found")
        }
    }
}
