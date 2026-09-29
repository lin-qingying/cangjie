package org.cangnova.cangjie.psi

import com.intellij.psi.stubs.ObjectStubSerializer
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.AbstractStringEnumerator
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJieImportDirectiveStub
import org.cangnova.cangjie.psi.stubs.CangJieImportGroupStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** 往返后的文件 Stub 没有关联源文件；导入 API 必须完全由 Stub 子树回答。 */
class ImportGroupStubTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun testRoundTripKeepsReferencesAndPathsWithoutSourceAst() {
        val restored = detachedStubs("""
            import org1::a.B
            import org1::a.{B as C, d.*}
            import org1::{a.B, c.D}
            import {a.B, c.D}
            import a.{a.B}
        """.trimIndent())
        assertNull(restored.psi, "反序列化根没有源文件，无法隐式加载原始 AST")
        val directives = descendants(restored).filterIsInstance<CangJieImportDirectiveStub>().map { it.psi }.toList()
        val items = directives.flatMap { it.importItems }
        assertEquals(listOf("a.B", "a.B", "a.d", "a.B", "c.D", "a.B", "c.D", "a.a.B"),
            items.map { it.importedFqName?.asString() })
        assertEquals(listOf("org1", "org1", "org1", "org1", "org1", null, null, null),
            items.map { it.organizationName?.asString() })
        assertTrue(items.all { it.isValidImport })
        assertEquals("C", items[1].aliasName)
        assertTrue(items[2].isAllUnder)
        assertEquals("org1", items[0].organizationReference?.referencedName)
        assertEquals("a.B", items[0].localFqName?.asString())

        val groups = descendants(restored).filterIsInstance<CangJieImportGroupStub>().map { it.psi }.toList()
        assertEquals("org1", groups[0].organizationReference?.referencedName)
        assertEquals("a", (groups[0].importedReference as CjSimpleNameExpression).referencedName)
        assertEquals("B", (groups[0].importItems[0].importedReference as CjSimpleNameExpression).referencedName)
        assertNull(groups[0].importItems[0].organizationReference)
        assertSame(groups[0], groups[0].importItems[0].importGroup)
        assertSame(directives[1], groups[0].importItems[0].importDirective)
        assertEquals("org1", groups[1].organizationReference?.referencedName)
        assertNull(groups[1].importedReference)
        assertNull(groups[1].localFqName)
        assertNull(groups[2].organizationReference)
        assertNull(groups[2].importedReference)
        assertNull(restored.psi)
    }

    @Test
    fun testLocalAndGroupQualifiedReferencesKeepTheirTreeAfterSerialization() {
        val restored = detachedStubs("import org1::a /* receiver */ . b.{c /* local */ .D}")
        val group = descendants(restored).filterIsInstance<CangJieImportGroupStub>().single().psi
        val prefix = group.importedReference as CjDotQualifiedExpression
        assertEquals("a", (prefix.receiverExpression as CjSimpleNameExpression).referencedName)
        assertEquals("b", (prefix.selectorExpression as CjSimpleNameExpression).referencedName)
        val item = group.importItems.single()
        val local = item.importedReference as CjDotQualifiedExpression
        assertEquals("c", (local.receiverExpression as CjSimpleNameExpression).referencedName)
        assertEquals("D", (local.selectorExpression as CjSimpleNameExpression).referencedName)
        assertEquals("a.b.c.D", item.importedFqName?.asString())
        assertNull(restored.psi)
    }

    @Test
    fun testMissingSelectorKeepsQualifiedReferenceAndInvalidityWithoutAst() {
        val restored = detachedStubs("import org1::a.{b., C}\nimport org1::a.b.\nimport recovery.K")
        val directives = descendants(restored).filterIsInstance<CangJieImportDirectiveStub>().map { it.psi }.toList()
        val missingLocal = directives[0].importItems.first()
        val localReference = missingLocal.importedReference as CjDotQualifiedExpression
        assertEquals("b", (localReference.receiverExpression as CjSimpleNameExpression).referencedName)
        assertNull(localReference.selectorExpression)
        assertFalse(missingLocal.isValidImport)
        assertNull(missingLocal.importedFqName)
        assertEquals("a.C", directives[0].importItems.last().importedFqName?.asString())
        val missingOuter = directives[1].importItems.single()
        val outerReference = missingOuter.importedReference as CjDotQualifiedExpression
        val receiver = outerReference.receiverExpression as CjDotQualifiedExpression
        assertEquals("a", (receiver.receiverExpression as CjSimpleNameExpression).referencedName)
        assertEquals("b", (receiver.selectorExpression as CjSimpleNameExpression).referencedName)
        assertNull(outerReference.selectorExpression)
        assertFalse(missingOuter.isValidImport)
        assertEquals("recovery.K", directives[2].importItems.single().importedFqName?.asString())
        assertNull(restored.psi)
    }

    @Test
    fun testRoundTripKeepsInvalidItemAndGroupBoundaries() {
        val restored = detachedStubs("""
            import a.{B as, C}
            import a.{B,,C}
            import recovery.K
        """.trimIndent())
        val directives = descendants(restored).filterIsInstance<CangJieImportDirectiveStub>().map { it.psi }.toList()
        val firstItems = directives[0].importItems
        assertEquals(2, firstItems.size)
        assertFalse(firstItems[0].isValidImport)
        assertNull(firstItems[0].importedFqName)
        assertTrue(firstItems[1].isValidImport)
        assertEquals("a.C", firstItems[1].importedFqName?.asString())
        assertTrue(directives[1].importItems.all { !it.isValidImport && it.importedFqName == null })
        assertEquals("recovery.K", directives[2].importItems.single().importedFqName?.asString())
        assertNull(restored.psi)
    }

    private fun detachedStubs(source: String): CangJieFileStub {
        val file = createPsiFile("importStubRoundTrip", source) as CjFile
        return roundTrip(CjFileStubBuilder().buildStubTree(file)) as CangJieFileStub
    }

    private fun descendants(stub: StubElement<*>): Sequence<StubElement<*>> = sequence {
        for (child in stub.childrenStubs) {
            yield(child)
            yieldAll(descendants(child))
        }
    }

    /** 逐节点使用生产 serializer，并确认没有遗留未消费的协议字节。 */
    private fun roundTrip(
        original: StubElement<*>,
        parent: StubElement<*>? = null,
        strings: AbstractStringEnumerator = TestStringEnumerator(),
    ): StubElement<*> {
        @Suppress("DEPRECATION")
        val serializer = if (original is PsiFileStub<*>) original.type else original.stubType
        @Suppress("UNCHECKED_CAST")
        serializer as ObjectStubSerializer<StubElement<*>, StubElement<*>>
        val bytes = ByteArrayOutputStream()
        serializer.serialize(original, StubOutputStream(bytes, strings))
        val input = StubInputStream(ByteArrayInputStream(bytes.toByteArray()), strings)
        val restored = serializer.deserialize(input, parent)
        assertEquals(-1, input.read())
        for (child in original.childrenStubs) roundTrip(child, restored, strings)
        return restored
    }

    private class TestStringEnumerator : AbstractStringEnumerator {
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
