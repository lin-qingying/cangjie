package org.cangnova.cangjie.psi

import com.intellij.psi.stubs.ObjectStubSerializer
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.AbstractStringEnumerator
import com.intellij.util.io.DataInputOutputUtil
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.stubs.CangJieCompiledDeclarationMetadata
import org.cangnova.cangjie.psi.stubs.CangJieCompiledDeclarationStatus
import org.cangnova.cangjie.psi.stubs.CangJieCompiledDeclarationStatus.BodyPresence
import org.cangnova.cangjie.psi.stubs.CangJieCompiledDeclarationStatus.Explicitness
import org.cangnova.cangjie.psi.stubs.CangJieCompiledDeclarationStatus.Flag
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieModifierListStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieNamedFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.ModifierMaskUtils
import org.cangnova.cangjie.psi.stubs.impl.deepCopy
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 独立验证完整语义状态的持久化协议，不把语法 modifier mask 当成状态事实。 */
class CompiledDeclarationStatusStubTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun testDetachedRoundTripAndCopyRetainEveryStatusField() {
        // 此用例只枚举 codec 字段，全部 Flag 同时置位不是合法声明语义 fixture。
        val status = CangJieCompiledDeclarationStatus(
            Visibilities.Unknown, Modality.SEALED, Explicitness.UNKNOWN, Explicitness.IMPLICIT,
            BodyPresence.ABSENT, Flag.entries.toSet(),
        )
        val getter = status.copy(visibility = Visibilities.Private, modality = Modality.FINAL,
            flags = setOf(Flag.STATIC, Flag.CONST))
        val setter = status.copy(visibility = Visibilities.Internal, modality = Modality.OPEN,
            flags = setOf(Flag.MUT, Flag.DEFAULT, Flag.COMMON_WITH_DEFAULT))
        val parameter = org.cangnova.cangjie.psi.stubs.CangJieCompiledSetterParameter(
            "newName", false, false, false, status.copy(visibility = Visibilities.Local),
        )
        val metadata = CangJieCompiledDeclarationMetadata(status, getter, setter, 3023, parameter)
        val original = rootWithHeader(metadata)
        val copied = original.deepCopy()
        val restored = roundTrip(copied) as CangJieFileStubImpl
        assertNull(restored.psi, "往返后的根没有原始 PSI，不能借 AST 恢复任何状态")
        val header = descendants(restored).filterIsInstance<CangJieModifierListStubImpl>().single()
        assertEquals(metadata, header.compiledDeclarationMetadata)
        assertNotSame(descendants(original).filterIsInstance<CangJieModifierListStubImpl>().single(), header)
        assertEquals(0L, header.mask, "完整状态没有被写进语法 token mask")
        assertTrue(Flag.ABSTRACT in header.compiledDeclarationMetadata!!.status.flags)
        assertTrue(!header.hasModifier(CjTokens.ABSTRACT_KEYWORD))
        val plainHeader = descendants(rootWithHeader(null)).filterIsInstance<CangJieModifierListStubImpl>().single()
        assertEquals(
            expected = plainHeader.toString(),
            actual = header.toString(),
            message = "语法 stub dump 不呈现独立的 binary 状态 payload",
        )
    }

    @Test
    fun testDistinctDeclarationsNeverExchangeMetadataWhenMasksMatch() {
        val first = CangJieCompiledDeclarationStatus(
            Visibilities.Public, Modality.ABSTRACT, Explicitness.UNKNOWN, Explicitness.UNKNOWN,
            BodyPresence.UNKNOWN, setOf(Flag.STATIC, Flag.ABSTRACT),
        )
        val second = first.copy(modality = Modality.FINAL, flags = setOf(Flag.STATIC))
        val restoredFirst = roundTrip(rootWithHeader(CangJieCompiledDeclarationMetadata(first)))
        val restoredSecond = roundTrip(rootWithHeader(CangJieCompiledDeclarationMetadata(second)))
        val firstHeader = descendants(restoredFirst).filterIsInstance<CangJieModifierListStubImpl>().single()
        val secondHeader = descendants(restoredSecond).filterIsInstance<CangJieModifierListStubImpl>().single()
        assertEquals(firstHeader.mask, secondHeader.mask)
        assertEquals(first, firstHeader.compiledDeclarationMetadata!!.status)
        assertEquals(second, secondHeader.compiledDeclarationMetadata!!.status)
    }

    @Test
    fun testSourceModifierRoundTripDoesNotInventBinaryStatus() {
        val file = createPsiFile("sourceStatus", "public func sample(): Unit {}") as CjFile
        val restored = roundTrip(CjFileStubBuilder().buildStubTree(file))
        val header = descendants(restored).filterIsInstance<CangJieModifierListStubImpl>()
            .single { it.hasModifier(CjTokens.PUBLIC_KEYWORD) }
        assertNull(header.compiledDeclarationMetadata)
        assertTrue(ModifierMaskUtils.maskHasModifier(header.mask, CjTokens.PUBLIC_KEYWORD))
    }

    @Test
    fun testUnknownStatusFlagIsRejectedInsteadOfReturningAnOrdinaryStatus() {
        val strings = Strings()
        val bytes = ByteArrayOutputStream()
        StubOutputStream(bytes, strings).apply {
            DataInputOutputUtil.writeLONG(this, 0L)
            writeBoolean(true)
            writeName(Visibilities.Public.name)
            writeByte(Modality.FINAL.ordinal)
            writeByte(Explicitness.UNKNOWN.ordinal)
            writeByte(Explicitness.UNKNOWN.ordinal)
            writeByte(BodyPresence.UNKNOWN.ordinal)
            DataInputOutputUtil.writeLONG(this, 1L shl 50)
        }
        assertFailsWith<IOException> {
            CjStubElementTypes.MODIFIER_LIST.deserialize(
                StubInputStream(ByteArrayInputStream(bytes.toByteArray()), strings),
                CangJieFileStubImpl.forFile(FqName("sample")),
            )
        }
    }

    @Test
    fun testEveryInvalidStatusOrdinalIsRejectedAsAProtocolError() {
        for (field in 0 until 4) {
            val strings = Strings()
            val bytes = ByteArrayOutputStream()
            StubOutputStream(bytes, strings).apply {
                DataInputOutputUtil.writeLONG(this, 0L)
                writeBoolean(true)
                writeName(Visibilities.Public.name)
                for (index in 0 until 4) writeByte(if (index == field) 255 else 0)
                DataInputOutputUtil.writeLONG(this, 0L)
                writeBoolean(false)
                writeBoolean(false)
                writeBoolean(false)
                writeBoolean(false)
            }
            val failure = assertFailsWith<IOException> {
                CjStubElementTypes.MODIFIER_LIST.deserialize(
                    StubInputStream(ByteArrayInputStream(bytes.toByteArray()), strings),
                    CangJieFileStubImpl.forFile(FqName("sample")),
                )
            }
            assertTrue(failure.message.orEmpty().contains("ordinal: 255"))
        }
    }

    private fun rootWithHeader(metadata: CangJieCompiledDeclarationMetadata?): CangJieFileStubImpl {
        val root = CangJieFileStubImpl.forFile(FqName("sample"))
        val function = CangJieNamedFunctionStubImpl(root, CjStubElementTypes.FUNCTION,
            StringRef.fromString("read"), true, FqName("sample.read"), false, false, false, null)
        CangJieModifierListStubImpl(function, 0L, CjStubElementTypes.MODIFIER_LIST, metadata)
        return root
    }

    private fun descendants(stub: StubElement<*>): Sequence<StubElement<*>> = sequence {
        for (child in stub.childrenStubs) {
            yield(child)
            yieldAll(descendants(child))
        }
    }

    private fun roundTrip(original: StubElement<*>, parent: StubElement<*>? = null,
        strings: AbstractStringEnumerator = Strings()): StubElement<*> {
        @Suppress("DEPRECATION")
        val serializer = if (original is PsiFileStub<*>) original.type else original.stubType
        @Suppress("UNCHECKED_CAST")
        serializer as ObjectStubSerializer<StubElement<*>, StubElement<*>>
        val bytes = ByteArrayOutputStream()
        serializer.serialize(original, StubOutputStream(bytes, strings))
        val input = StubInputStream(ByteArrayInputStream(bytes.toByteArray()), strings)
        val restored = serializer.deserialize(input, parent)
        assertEquals(-1, input.read(), "生产 serializer 必须消费完整状态段")
        original.childrenStubs.forEach { roundTrip(it, restored, strings) }
        return restored
    }

    private class Strings : AbstractStringEnumerator {
        private val ids = HashMap<String, Int>()
        private val values = mutableListOf<String>()
        override fun enumerate(value: String?): Int =
            if (value == null) 0 else ids.getOrPut(value) { values += value; values.size }
        override fun valueOf(idx: Int): String? = if (idx == 0) null else values[idx - 1]
        override fun markCorrupted(): Unit = error("Unexpected persistent storage operation")
        override fun close(): Unit = error("Unexpected persistent storage operation")
        override fun isDirty(): Boolean = error("Unexpected persistent storage operation")
        override fun force(): Unit = error("Unexpected persistent storage operation")
    }
}
