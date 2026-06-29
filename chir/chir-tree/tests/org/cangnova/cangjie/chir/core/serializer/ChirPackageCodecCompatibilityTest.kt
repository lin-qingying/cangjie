package org.cangnova.cangjie.chir.core.serializer

import PackageFormat.CHIRPackage
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.chir.core.testkit.ChirTestAssertions
import org.cangnova.cangjie.chir.core.testkit.ChirTestFixtures
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/**
 * 校验 CHIR 包编解码器与当前 FlatBuffers schema 的兼容性边界。
 *
 * 该测试覆盖正常往返、schema 版本拒绝和损坏载荷拒绝，确保序列化入口不会静默接受不兼容数据。
 */
class ChirPackageCodecCompatibilityTest {

    /**
     * 校验当前 schema 下序列化再反序列化能够保持语义等价。
     *
     * 该用例通过测试工具断言样本包的结构、标识和关键字段在往返后不发生漂移。
     */
    @Test
    fun `round trip keeps semantic equivalence for current schema`() {
        ChirTestAssertions.assertCodecRoundTrip(ChirTestFixtures.codecPackage())
    }

    /**
     * 校验反序列化会拒绝高于当前支持版本的 schema。
     *
     * 该用例直接篡改 FlatBuffers 根节点版本字段，固定版本兼容策略的失败行为。
     */
    @Test
    fun `deserialize rejects unsupported schema version`() {
        val bytes = ChirPackageCodec.serialize(ChirTestFixtures.codecPackage())
        val root = CHIRPackage.getRootAsCHIRPackage(ByteBuffer.wrap(bytes))
        root.mutatePhase((ChirSerializationSchema.CURRENT_VERSION + 1).toUByte())

        assertThrows(ChirSerializationException::class.java) {
            ChirPackageCodec.deserialize(bytes)
        }
    }

    /**
     * 校验反序列化会拒绝结构损坏的载荷。
     *
     * 该用例构造字段不完整的包文本，确保解码失败会以 `ChirSerializationException` 暴露。
     */
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
