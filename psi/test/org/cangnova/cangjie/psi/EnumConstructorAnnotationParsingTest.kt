package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.stubs.ObjectStubSerializer
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.io.AbstractStringEnumerator
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.stubs.CangJieEnumConstructorStub
import org.cangnova.cangjie.psi.stubs.CangJieEnumStub
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJieMacroExpressionStub
import org.cangnova.cangjie.psi.stubs.CangJieStubElement
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
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

/**
 * 枚举项上的注解链必须保留枚举作用域、payload 类型和后续成员边界。
 *
 * 语法依据官方 896235c9 的 ParseDecl.cpp:1167–1245、1446–1499，
 * 以及 ParseMacro.cpp:614–617 的 ENUM_CONSTRUCTOR 作用域传播。
 */
class EnumConstructorAnnotationParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /** 普通注解的宏包装不得把带参枚举项改写为主构造器。 */
    @Test
    fun customAnnotationChainsKeepEnumConstructorsAndMembers() {
        val file = parse(
            "enumAnnotationChains",
            """
            enum E {
                @CaseTag[1]
                @CaseVersion[]
                caseA
                | @CaseTag[2]
                @CaseVersion[]
                caseB(Int64)
                | @!CaseTag[3]
                caseC(Int64, Bool)

                @MemberTag[]
                public func number(): Int64 { 1 }

                @MemberTag[]
                public prop value: Int64 { get() { 2 } }
            }
            """.trimIndent(),
        )
        val enum = file.declarations.filterIsInstance<CjEnum>().single()
        val constructors = enum.constructor
        assertEquals(listOf("caseA", "caseB", "caseC"), constructors.map { it.name })
        assertEquals(
            listOf(emptyList(), listOf("Int64"), listOf("Int64", "Bool")),
            constructors.map { constructor -> constructor.typeReferences.map { it.text } },
        )
        constructors.forEach { assertSame(enum, it.parentEnum) }
        assertTrue(PsiTreeUtil.findChildrenOfType(enum, CjPrimaryConstructor::class.java).isEmpty())
        assertTrue(constructors.take(2).all { it.annotationEntries.isEmpty() })
        val directAnnotation = constructors[2].annotationEntries.single()
        assertEquals("@!CaseTag[3]", directAnnotation.text)
        assertTrue(directAnnotation.isCompileTimeVisible)

        val function = PsiTreeUtil.findChildrenOfType(enum, CjNamedFunction::class.java).single()
        val property = PsiTreeUtil.findChildrenOfType(enum, CjProperty::class.java).single()
        assertEquals("number", function.name)
        assertEquals("value", property.name)

        // 普通 @ 保持宏调用语法；真实 CjAnnotation 只承载 parser 已确认的注解。
        val wrappers = checkNotNull(enum.body).children.filterIsInstance<CjMacroExpression>()
        assertEquals(4, wrappers.size)
        assertMacroChain(wrappers[0], listOf("CaseTag" to "[1]", "CaseVersion" to "[]"), constructors[0])
        assertMacroChain(wrappers[1], listOf("CaseTag" to "[2]", "CaseVersion" to "[]"), constructors[1])
        assertMacroChain(wrappers[2], listOf("MemberTag" to "[]"), function)
        assertMacroChain(wrappers[3], listOf("MemberTag" to "[]"), property)
    }

    /** 可选的首个竖线不改变带参首项和无参后续项的节点类型。 */
    @Test
    fun optionalLeadingBarKeepsTheAnnotatedPayloadOnTheFirstConstructor() {
        for (leadingBar in listOf("", "|")) {
            val file = parse(
                "enumAnnotationLeadingBar${leadingBar.length}",
                """
                enum E {
                    $leadingBar @CaseTag[1]
                    @CaseVersion[]
                    first(Int64)
                    | @CaseTag[2]
                    empty
                }
                """.trimIndent(),
            )
            val enum = file.declarations.filterIsInstance<CjEnum>().single()
            val constructors = enum.constructor
            assertEquals(listOf("first", "empty"), constructors.map { it.name })
            assertEquals(
                listOf(listOf("Int64"), emptyList()),
                constructors.map { it.typeReferences.map { type -> type.text } },
            )
            assertTrue(constructors.all { it.annotationEntries.isEmpty() })
            val wrappers = checkNotNull(enum.body).children.filterIsInstance<CjMacroExpression>()
            assertEquals(2, wrappers.size)
            assertMacroChain(wrappers[0], listOf("CaseTag" to "[1]", "CaseVersion" to "[]"), constructors[0])
            assertMacroChain(wrappers[1], listOf("CaseTag" to "[2]"), constructors[1])
            assertTrue(PsiTreeUtil.findChildrenOfType(enum, CjPrimaryConstructor::class.java).isEmpty())
        }
    }

    /** @! 使用真实注解节点，不经过普通 @ 的宏包装链。 */
    @Test
    fun forcedCustomAnnotationStaysAttachedToTheEnumConstructor() {
        val file = parse(
            "forcedEnumAnnotation",
            """
            enum E {
                @!CaseTag[1]
                first(Int64)
                | empty
            }
            """.trimIndent(),
        )
        val enum = file.declarations.filterIsInstance<CjEnum>().single()
        val constructors = enum.constructor
        assertEquals(listOf("first", "empty"), constructors.map { it.name })
        val annotation = constructors[0].annotationEntries.single()
        assertEquals("CaseTag", annotation.shortName?.asString())
        assertEquals("@!CaseTag[1]", annotation.text)
        assertTrue(annotation.isCompileTimeVisible)
        assertTrue(constructors[1].annotationEntries.isEmpty())
        assertTrue(PsiTreeUtil.findChildrenOfType(enum, CjMacroExpression::class.java).isEmpty())
    }

    /** 枚举 body → 宏 → 枚举项的 stub 层次和输入类别必须经过真实序列化保留。 */
    @Test
    fun enumConstructorOwnershipSurvivesStubGenerationAndSerialization() {
        val file = parse(
            "enumAnnotationStubs",
            """
            package annotationstubs

            enum E {
                @Outer[1]
                @Inner[2]
                first(Int64)
                | @Outer[3]
                empty
            }
            """.trimIndent(),
        )
        val original = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        assertEnumStubOwnership(original)
        assertEnumStubOwnership(roundTripStub(original) as CangJieFileStub)
    }

    /** 官方 ParseMacro.cpp:439–456 的括号输入为 token，不能穿透其中的声明宏。 */
    @Test
    fun parenthesizedMacroInputDoesNotExposeAQuotedDeclaration() {
        val file = parse(
            "quotedMacroDeclaration",
            """
            @Outer(@Inner class Quoted {})
            func following(): Unit {}
            """.trimIndent(),
        )
        val outer = file.children.filterIsInstance<CjMacroExpression>().single()
        val inner = PsiTreeUtil.findChildrenOfType(outer, CjMacroExpression::class.java).single()
        assertFalse(outer.hasDeclarationInput)
        assertNull(outer.unwrappedDeclaration)
        assertTrue(inner.hasDeclarationInput)
        assertTrue(inner.unwrappedDeclaration is CjClass)
        assertEquals(listOf("following"), file.declarations.filterIsInstance<CjNamedFunction>().map { it.name })
        assertTrue(file.declarations.none { it is CjClass })

        val original = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        assertQuotedStubBoundary(original)
        assertQuotedStubBoundary(roundTripStub(original) as CangJieFileStub)
    }

    private fun assertMacroChain(
        outer: CjMacroExpression,
        expected: List<Pair<String, String>>,
        declaration: CjDeclaration,
    ) {
        var current = outer
        for ((index, nameAndAttr) in expected.withIndex()) {
            assertEquals(nameAndAttr.first, current.shortName?.asString())
            assertEquals(nameAndAttr.second, current.attr?.text)
            assertTrue(current.hasDeclarationInput)
            assertSame(declaration, current.unwrappedDeclaration)
            val input = checkNotNull(current.input)
            if (index == expected.lastIndex) {
                assertSame(declaration, input.declarations)
            } else {
                current = input.children.filterIsInstance<CjMacroExpression>().single()
            }
        }
    }

    private fun assertEnumStubOwnership(fileStub: CangJieFileStub) {
        val enumStub = fileStub.childrenStubs.filterIsInstance<CangJieEnumStub>().single()
        val bodyStub = enumStub.childrenStubs.single { it.stubType == CjStubElementTypes.ENUM_BODY }
        val wrappers = bodyStub.childrenStubs.filterIsInstance<CangJieMacroExpressionStub>()
        assertEquals(listOf("Outer", "Outer"), wrappers.map { it.getShortName() })
        val inner = wrappers[0].childrenStubs.filterIsInstance<CangJieMacroExpressionStub>().single()
        assertEquals("Inner", inner.getShortName())
        val first = inner.childrenStubs.filterIsInstance<CangJieEnumConstructorStub>().single()
        val empty = wrappers[1].childrenStubs.filterIsInstance<CangJieEnumConstructorStub>().single()
        assertEquals(listOf("first", "empty"), listOf(first.name, empty.name))
        assertEquals(listOf(1, 0), listOf(first.getTypeCount(), empty.getTypeCount()))
        assertEquals("annotationstubs.E", first.getEnumFqName()?.asString())
        assertEquals("annotationstubs.E", empty.getEnumFqName()?.asString())

        val enum = enumStub.psi
        assertEquals(listOf("first", "empty"), enum.constructor.map { it.name })
        assertSame(first.psi, enum.constructor[0])
        assertSame(empty.psi, enum.constructor[1])
        assertSame(enum, first.psi.parentEnum)
        assertSame(enum, empty.psi.parentEnum)
        val copiedFile = roundTripStub(fileStub.copyInto(null)) as CangJieFileStub
        for ((wrapper, expectedConstructor) in listOf(wrappers[0] to first, inner to first, wrappers[1] to empty)) {
            assertTrue(wrapper.hasDeclarationInput())
            assertTrue(wrapper.psi.hasDeclarationInput)
            assertSame(expectedConstructor.psi, wrapper.psi.unwrappedDeclaration)
            val copied = (wrapper as CangJieStubElement<*>).copyInto(copiedFile) as CangJieMacroExpressionStub
            assertSame(copiedFile, copied.parentStub)
            assertTrue(copied.hasDeclarationInput())
        }
    }

    private fun assertQuotedStubBoundary(fileStub: CangJieFileStub) {
        val outer = fileStub.childrenStubs.filterIsInstance<CangJieMacroExpressionStub>().single()
        val inner = outer.childrenStubs.filterIsInstance<CangJieMacroExpressionStub>().single()
        assertEquals("Outer", outer.getShortName())
        assertEquals("Inner", inner.getShortName())
        assertFalse(outer.hasDeclarationInput())
        assertFalse(outer.psi.hasDeclarationInput)
        assertNull(outer.psi.unwrappedDeclaration)
        assertTrue(inner.hasDeclarationInput())
        assertTrue(inner.psi.hasDeclarationInput)
        assertTrue(inner.psi.unwrappedDeclaration is CjClass)
        val copiedFile = roundTripStub(fileStub.copyInto(null)) as CangJieFileStub
        val copied = (outer as CangJieStubElement<*>).copyInto(copiedFile) as CangJieMacroExpressionStub
        assertSame(copiedFile, copied.parentStub)
        assertFalse(copied.hasDeclarationInput())
        assertNull(copied.psi.unwrappedDeclaration)
    }

    /** 对齐 Kotlin StubsTestEngine：逐节点调用真实 serializer，并检查读取完整性与子树。 */
    private fun roundTripStub(
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
        assertEquals(-1, input.read(), "stub serializer and deserializer must consume the same bytes")
        assertEquals(original::class, restored::class)
        @Suppress("DEPRECATION")
        assertEquals(original.stubType, restored.stubType)
        for (child in original.childrenStubs) {
            roundTripStub(child, restored, strings)
        }
        return restored
    }

    /** 仅供 stub 字符串引用的进程内编号，0 与官方 stream 协议一样表示 null。 */
    private class TestStringEnumerator : AbstractStringEnumerator {
        private val values = HashMap<String, Int>()
        private val strings = mutableListOf<String>()

        override fun enumerate(value: String?): Int {
            if (value == null) return 0
            return values.getOrPut(value) {
                strings += value
                values.size + 1
            }
        }

        override fun valueOf(idx: Int): String? = if (idx == 0) null else strings[idx - 1]
        override fun markCorrupted(): Unit = error("Unexpected persistent storage operation")
        override fun close(): Unit = error("Unexpected persistent storage operation")
        override fun isDirty(): Boolean = error("Unexpected persistent storage operation")
        override fun force(): Unit = error("Unexpected persistent storage operation")
    }

    private fun parse(name: String, source: String): CjFile {
        val file = createPsiFile(name, source) as CjFile
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(errors.isEmpty(), errors.joinToString { it.errorDescription })
        return file
    }
}
