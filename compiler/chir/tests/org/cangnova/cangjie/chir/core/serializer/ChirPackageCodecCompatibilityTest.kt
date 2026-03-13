package org.cangnova.cangjie.chir.core.serializer

import PackageFormat.CHIRPackage
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.chir.core.testkit.ChirTestAssertions
import org.cangnova.cangjie.chir.core.testkit.ChirTestFixtures
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class ChirPackageCodecCompatibilityTest {

    @Test
    fun `round trip keeps semantic equivalence for current schema`() {
        ChirTestAssertions.assertCodecRoundTrip(ChirTestFixtures.codecPackage())
    }

    @Test
    fun `deserialize rejects unsupported schema version`() {
        val bytes = ChirPackageCodec.serialize(ChirTestFixtures.codecPackage())
        val root = CHIRPackage.getRootAsCHIRPackage(ByteBuffer.wrap(bytes))
        root.mutatePhase((ChirSerializationSchema.CURRENT_VERSION + 1).toUByte())

        assertThrows(ChirSerializationException::class.java) {
            ChirPackageCodec.deserialize(bytes)
        }
    }

    @Test
    fun `deserialize rejects damaged payload`() {
        val builder = FlatBufferBuilder(256)
        val nameOffset = builder.createString("bad")
        val pathOffset = builder.createString("S|1\nP|pkg:bad|bad\nM|mod:bad|bad\nF|broken")
        val packageOffset = CHIRPackage.createCHIRPackage(
            builder = builder,
            nameOffset = nameOffset,
            pathOffset = pathOffset,
            pkgAccessLevel = 0u,
            typesTypeOffset = 0,
            typesOffset = 0,
            valuesTypeOffset = 0,
            valuesOffset = 0,
            exprsTypeOffset = 0,
            exprsOffset = 0,
            defsTypeOffset = 0,
            defsOffset = 0,
            packageInitFunc = 0u,
            phase = ChirSerializationSchema.CURRENT_VERSION.toUByte(),
            packageLiteralInitFunc = 0u,
            maxImportedValueId = 0u,
            maxImportedStructId = 0u,
            maxImportedClassId = 0u,
            maxImportedEnumId = 0u,
            maxImportedExtendId = 0u,
        )
        CHIRPackage.finishCHIRPackageBuffer(builder, packageOffset)

        assertThrows(ChirSerializationException::class.java) {
            ChirPackageCodec.deserialize(builder.sizedByteArray())
        }
    }
}
