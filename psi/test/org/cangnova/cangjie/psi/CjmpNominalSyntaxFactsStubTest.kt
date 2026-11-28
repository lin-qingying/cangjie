package org.cangnova.cangjie.psi

import com.intellij.psi.stubs.ObjectStubSerializer
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.AbstractStringEnumerator
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.CjmpNominalSyntaxFacts
import org.cangnova.cangjie.parsing.CjmpDirectMemberSyntaxFacts
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJieStubElement
import org.cangnova.cangjie.psi.stubs.CangJieTypeStatementStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.psi.stubs.elements.readCjmpNominalSyntaxFacts
import org.cangnova.cangjie.psi.stubs.elements.writeCjmpNominalSyntaxFacts
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class CjmpNominalSyntaxFactsStubTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun sourceFactsSurviveAllContainerStubSerializersAndCopies() {
        val file = createPsiFile("nominalFacts", """
            class Owner { func f(): Unit {} let x: Int64 = 1 }
            struct Value { func f(): Unit {} }
            interface Face { func f(): Unit }
            enum Choice { First | Second }
            extend Value { func f(): Unit {} }
        """.trimIndent()) as CjFile
        val declarations = file.declarations.filterIsInstance<CjTypeStatement>()
        assertEquals(5, declarations.size)
        val expected = declarations.map { requireNotNull(it.cjmpNominalSyntaxFacts) }
        val original = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        val stubs = original.childrenStubs.filterIsInstance<CangJieTypeStatementStub<*>>()
        assertEquals(expected, stubs.map { it.cjmpNominalSyntaxFacts })
        val copyFile = createPsiFile("copyParent", "class CopyParent {}") as CjFile
        val copyParent = CjFileStubBuilder().buildStubTree(copyFile) as CangJieFileStub
        for (stub in stubs) {
            val copy = (stub as CangJieStubElement<*>).copyInto(copyParent) as CangJieTypeStatementStub<*>
            assertSame(copyParent, copy.parentStub)
            assertEquals(stub.cjmpNominalSyntaxFacts, copy.cjmpNominalSyntaxFacts)
        }
        val restored = roundTrip(original) as CangJieFileStub
        assertNull(restored.psi)
        val restoredStubs = restored.childrenStubs.filterIsInstance<CangJieTypeStatementStub<*>>()
        assertEquals(expected, restoredStubs.map { it.cjmpNominalSyntaxFacts })
        assertEquals(expected, restoredStubs.map { it.psi.cjmpNominalSyntaxFacts })
        assertNull(restored.psi)
    }

    @Test
    fun absentAndEmptySnapshotsAreDistinct() {
        val file = createPsiFile("emptyNominalFacts", "class Empty {}") as CjFile
        val original = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        val stub = original.childrenStubs.filterIsInstance<CangJieTypeStatementStub<*>>().single()
        assertEquals(CjmpNominalSyntaxFacts(emptyList()), stub.cjmpNominalSyntaxFacts)
        val restored = roundTrip(original) as CangJieFileStub
        assertEquals(CjmpNominalSyntaxFacts(emptyList()),
            restored.childrenStubs.filterIsInstance<CangJieTypeStatementStub<*>>().single().cjmpNominalSyntaxFacts)
        val binaryLike = org.cangnova.cangjie.psi.stubs.impl.CangJieClassStubImpl(
            org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.CLASS,
            original, null, null, null, emptyArray(),
        )
        assertNull(binaryLike.cjmpNominalSyntaxFacts)
        val copy = binaryLike.copyInto(restored)
        assertSame(restored, copy.parentStub)
        assertNull(copy.cjmpNominalSyntaxFacts)
    }

    @Test
    fun wireFormatPreservesAbsenceTypesAndEveryFlag() {
        val facts = CjmpNominalSyntaxFacts(listOf(
            CjmpDirectMemberSyntaxFacts(CjNodeTypes.FUNC, true, false, true, false, true, false),
            CjmpDirectMemberSyntaxFacts(CjNodeTypes.PROPERTY, false, true, false, true, false, true),
            CjmpDirectMemberSyntaxFacts(CjNodeTypes.FIELD, false, false, false, false, false, false),
            CjmpDirectMemberSyntaxFacts(CjNodeTypes.CLASS, true, true, true, true, true, true),
        ))
        for (value in listOf(null, CjmpNominalSyntaxFacts(emptyList()), facts)) {
            val strings = TestStrings()
            val bytes = ByteArrayOutputStream()
            StubOutputStream(bytes, strings).use { writeCjmpNominalSyntaxFacts(it, value) }
            StubInputStream(ByteArrayInputStream(bytes.toByteArray()), strings).use {
                assertEquals(value, readCjmpNominalSyntaxFacts(it))
                assertEquals(-1, it.read())
            }
        }
    }

    private fun roundTrip(
        original: StubElement<*>,
        parent: StubElement<*>? = null,
        strings: AbstractStringEnumerator = TestStrings(),
    ): StubElement<*> {
        @Suppress("DEPRECATION")
        val serializer = if (original is PsiFileStub<*>) original.type else original.stubType
        @Suppress("UNCHECKED_CAST")
        serializer as ObjectStubSerializer<StubElement<*>, StubElement<*>>
        val bytes = ByteArrayOutputStream()
        StubOutputStream(bytes, strings).use { serializer.serialize(original, it) }
        val restored = StubInputStream(ByteArrayInputStream(bytes.toByteArray()), strings).use {
            serializer.deserialize(it, parent).also { _ -> assertEquals(-1, it.read()) }
        }
        for (child in original.childrenStubs) roundTrip(child, restored, strings)
        return restored
    }

    private class TestStrings : AbstractStringEnumerator {
        private val ids = HashMap<String, Int>()
        private val values = mutableListOf<String>()
        override fun enumerate(value: String?): Int {
            if (value == null) return 0
            return ids.getOrPut(value) { values += value; values.size }
        }
        override fun valueOf(idx: Int): String? = if (idx == 0) null else values[idx - 1]
        override fun markCorrupted(): Unit = error("Unexpected persistent storage operation")
        override fun close(): Unit = error("Unexpected persistent storage operation")
        override fun isDirty(): Boolean = error("Unexpected persistent storage operation")
        override fun force(): Unit = error("Unexpected persistent storage operation")
    }
}
