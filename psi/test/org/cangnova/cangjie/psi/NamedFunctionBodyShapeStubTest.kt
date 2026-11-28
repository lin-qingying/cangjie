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
import org.cangnova.cangjie.psi.stubs.CangJieNamedFunctionStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/** 空块是源码语法事实；持久化后的 PSI 必须只读 stub，不能加载函数体或依赖 CFIR。 */
class NamedFunctionBodyShapeStubTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun testAstDistinguishesEmptyBlocksFromNestedBlocksAndMissingBodies() {
        val file = createPsiFile("emptyBodySyntax", SOURCE) as CjFile
        val functions = file.declarations.filterIsInstance<CjNamedFunction>()
        assertEquals(EXPECTED, functions.associate { it.name to it.hasEmptyBlockBody() })
    }

    /** 复用 ImportGroupStubTest 的整树生产 serializer 往返，恢复根不携带任何源文件。 */
    @Test
    fun testEmptyBodyFactsSurviveStubSerializationWithoutLoadingSourceAst() {
        val file = createPsiFile("emptyBodyStub", SOURCE) as CjFile
        val astFacts = file.declarations.filterIsInstance<CjNamedFunction>()
            .associate { it.name to it.hasEmptyBlockBody() }
        assertEquals(EXPECTED, astFacts)

        val original = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        val sourceStubs = namedFunctions(original)
        assertEquals(EXPECTED, sourceStubs.associate { it.name to it.hasEmptyBlockBody() })

        val restored = roundTrip(original) as CangJieFileStub
        assertNull(restored.psi, "反序列化根没有源 PSI，查询不能借原始 AST 计算空体")
        val restoredStubs = namedFunctions(restored)
        assertEquals(EXPECTED, restoredStubs.associate { it.name to it.hasEmptyBlockBody() })
        for (stub in restoredStubs) {
            val psi = stub.psi
            assertSame(stub, psi.stub)
            assertEquals(EXPECTED.getValue(stub.name), psi.hasEmptyBlockBody(), "detached function ${stub.name}")
            assertSame(stub, psi.stub, "读取空体事实后 PSI 仍关联原 stub")
        }
        assertNull(restored.psi, "查询完成后不能物化源文件 AST")
    }

    private fun namedFunctions(root: StubElement<*>): List<CangJieNamedFunctionStub> =
        root.childrenStubs.filterIsInstance<CangJieNamedFunctionStub>()

    /** 每个节点都走对应的真实 serializer，验证新增字段没有导致协议读写错位。 */
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
        StubOutputStream(bytes, strings).use { serializer.serialize(original, it) }
        val restored = StubInputStream(ByteArrayInputStream(bytes.toByteArray()), strings).use { input ->
            serializer.deserialize(input, parent).also {
                assertEquals(-1, input.read(), "stub serializer/deserializer must consume identical fields")
            }
        }
        for (child in original.childrenStubs) roundTrip(child, restored, strings)
        return restored
    }

    /** 只给 stub stream 提供进程内字符串编号，不创建持久化索引。 */
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

    companion object {
        private val EXPECTED = linkedMapOf<String?, Boolean>(
            "empty" to true,
            "whitespaceOnly" to true,
            "blockCommentOnly" to true,
            "lineCommentOnly" to true,
            "nestedBlock" to false,
            "nestedCommentBlock" to false,
            "nonEmpty" to false,
            "bodyless" to false,
        )

        // bodyless 只验证“没有 body”这一语法事实，不声明它是通过语义检查的普通顶层函数。
        private val SOURCE = """
            package empty_body_stub

            func empty() {}
            func whitespaceOnly() {

            }
            func blockCommentOnly() { /* braces in comment: { 1 } */ }
            func lineCommentOnly() {
                // braces in comment: { 1 }
            }
            func nestedBlock() { {} }
            func nestedCommentBlock() { { /* comment inside nested block */ } }
            func nonEmpty() { 1 }
            func bodyless(): Unit
        """.trimIndent()
    }
}
