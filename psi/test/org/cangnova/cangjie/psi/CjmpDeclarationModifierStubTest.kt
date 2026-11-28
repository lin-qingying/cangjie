package org.cangnova.cangjie.psi

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.AstLoadingFilter
import com.intellij.util.io.AbstractStringEnumerator
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.CjModifierKind
import org.cangnova.cangjie.parsing.CjmpParsingContext
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.psi.psiUtil.parsedDeclarationModifiersOrNull
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJieStubElement
import org.cangnova.cangjie.psi.stubs.CangJieTypeStatementStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.psi.stubs.elements.CjmpFileRoleSnapshotCodec
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubKindImpl
import org.cangnova.cangjie.psi.stubs.impl.deepCopy
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertFailsWith as assertValueFails
import kotlin.test.assertNotNull as assertValuePresent
import kotlin.test.assertTrue as assertValueTrue

/** 实际 source stub、复制和编码往返均通过结构身份读取返回属性，禁止为此加载 AST。 */
class CjmpDeclarationModifierStubTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun sourceStubAndCopyRetainInheritedAndEmptyDeclarationResults() {
        val root = build("common foreign { class C {} }\nclass After {}")
        assertClassResults(root)
        val copy = root.deepCopy()
        AstLoadingFilter.disallowTreeLoading<Unit, Nothing> { assertClassResults(copy) }
        assertValueEquals(root.cjmpRoleSnapshot?.stubDeclarationModifiers, copy.cjmpRoleSnapshot?.stubDeclarationModifiers)
    }

    @Test
    fun codecRoundTripReadsActualDeclarationResultsWithoutAst() {
        val source = "common foreign { class C {} }\nclass After {}"
        val root = build(source)
        val snapshot = assertValuePresent(root.cjmpRoleSnapshot)
        val strings = TestStrings()
        val bytes = ByteArrayOutputStream()
        StubOutputStream(bytes, strings).use { CjmpFileRoleSnapshotCodec.write(it, snapshot) }
        val decoded = StubInputStream(ByteArrayInputStream(bytes.toByteArray()), strings).use { input ->
            assertValuePresent(CjmpFileRoleSnapshotCodec.read(input)).also { assertValueEquals(-1, input.read()) }
        }
        assertValueEquals(snapshot, decoded)
        assertValueEquals(true, decoded.matchesSourceContent(source))
        val restored = CangJieFileStubImpl(null, CangJieFileStubKindImpl.File(FqName.ROOT), decoded)
        copyChildren(root, restored)
        AstLoadingFilter.disallowTreeLoading<Unit, Nothing> { assertClassResults(restored) }
    }

    @Test
    fun repeatedStubBuildsPreserveTheExactCodecOrder() {
        val source = "common class A {}\ncommon class B {}\ncommon class C {}\nclass After {}"
        val file = createPsiFile("modifierOrder.cj", source) as CjFile
        file.cjmpParsingContext = CjmpParsingContext.Compilation(true, false, true)
        val first = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        val second = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        assertValueEquals(file.cjmpParsingContext, assertValuePresent(first.cjmpRoleSnapshot).parsingContext)
        fun encode(root: CangJieFileStub): ByteArray {
            val bytes = ByteArrayOutputStream()
            StubOutputStream(bytes, TestStrings()).use { CjmpFileRoleSnapshotCodec.write(it, assertValuePresent(root.cjmpRoleSnapshot)) }
            return bytes.toByteArray()
        }
        assertValueEquals(4, assertValuePresent(first.cjmpRoleSnapshot).stubDeclarationModifiers.size)
        assertValueEquals(first.cjmpRoleSnapshot?.stubDeclarationModifiers?.keys?.toList(), second.cjmpRoleSnapshot?.stubDeclarationModifiers?.keys?.toList())
        assertValueTrue(encode(first).contentEquals(encode(second)))
    }

    @Test
    fun corruptAttributeNameCannotDecodeAsAnEmptyOrLexicalResult() {
        val snapshot = assertValuePresent(build("public class C {}").cjmpRoleSnapshot)
        val strings = TestStrings()
        val bytes = ByteArrayOutputStream()
        StubOutputStream(bytes, strings).use { CjmpFileRoleSnapshotCodec.write(it, snapshot) }
        val payload = bytes.toByteArray()
        val original = "PUBLIC".toByteArray(Charsets.UTF_8)
        val invalid = "BOGUS!".toByteArray(Charsets.UTF_8)
        val offset = (0..payload.size - original.size).single { start ->
            original.indices.all { payload[start + it] == original[it] }
        }
        invalid.copyInto(payload, offset)
        StubInputStream(ByteArrayInputStream(payload), strings).use { input ->
            val failure = assertValueFails<IOException> { CjmpFileRoleSnapshotCodec.read(input) }
            assertValueEquals("Invalid declaration modifier attribute: BOGUS!", failure.message)
        }
    }

    private fun assertClassResults(root: CangJieFileStub) {
        val owners = allStubs(root).filterIsInstance<CangJieTypeStatementStub<*>>().associateBy { it.name }
        assertValueEquals(setOf("C", "After"), owners.keys)
        val inherited = assertValuePresent((owners.getValue("C").psi as CjDeclaration).parsedDeclarationModifiersOrNull())
        val empty = assertValuePresent((owners.getValue("After").psi as CjDeclaration).parsedDeclarationModifiersOrNull())
        assertValueEquals(setOf(CjModifierKind.COMMON, CjModifierKind.FOREIGN), inherited.attributes)
        assertValueEquals(emptySet(), empty.attributes)
        assertValueEquals(inherited.rootIdentity, empty.rootIdentity)
        assertValueTrue(inherited.role.isCommon)
        assertValueEquals(false, empty.role.isCommon)
    }

    private fun allStubs(root: StubElement<*>): List<StubElement<*>> =
        root.childrenStubs.flatMap { listOf(it) + allStubs(it) }

    private fun copyChildren(source: StubElement<*>, target: StubElement<*>) {
        for (child in source.childrenStubs) {
            val copy = (child as CangJieStubElement<*>).copyInto(target)
            copyChildren(child, copy)
        }
    }

    private fun build(source: String): CangJieFileStubImpl {
        val file = createPsiFile("declarationModifiers.cj", source) as CjFile
        file.cjmpParsingContext = CjmpParsingContext.Compilation(true, false, true)
        val root = CjFileStubBuilder().buildStubTree(file) as CangJieFileStubImpl
        assertValueEquals(file.cjmpParsingContext, assertValuePresent(root.cjmpRoleSnapshot).parsingContext)
        return root
    }

    private class TestStrings : AbstractStringEnumerator {
        private val values = mutableListOf<String>()
        private val indices = mutableMapOf<String, Int>()
        override fun enumerate(value: String?): Int = if (value == null) 0 else indices.getOrPut(value) { values += value; values.size }
        override fun valueOf(idx: Int): String? = if (idx == 0) null else values[idx - 1]
        override fun markCorrupted(): Unit = error("Unexpected persistent storage operation")
        override fun close(): Unit = error("Unexpected persistent storage operation")
        override fun isDirty(): Boolean = error("Unexpected persistent storage operation")
        override fun force(): Unit = error("Unexpected persistent storage operation")
    }
}
