package org.cangnova.cangjie.parsing

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * prop 体内重复 accessor 的 parser 行为。
 *
 * 期望值来自官方 cjc 1.0.5 的 JSON 诊断（`parse_duplicated_get_or_set`）：
 * 每个重复的同类 accessor 各报一条 `duplicated '<get|set>' in prop`，且重复节点**仍保留**
 * 在属性体中（官方 `ParsePropBody` 不跳过重复项）。仓库的 `PsiErrorElement` 为零宽节点，
 * 锚点落在重复关键字消费之后（同一行关键字末尾），而不是官方覆盖关键字的字符区间。
 *
 * 官方诊断另带 "previous one"（前一个 accessor 节点）与 prop 起始位置两个 hint；
 * `PsiErrorElement` 只有 errorDescription，无法携带 related location，故此处不断言 hint。
 */
class PropAccessorParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = CangJieFileType.EXTENSION,
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

    private fun parse(source: String): CjFile = createPsiFile("propAccessor", source) as CjFile

    private fun CjFile.errors(): List<PsiErrorElement> =
        PsiTreeUtil.findChildrenOfType(this, PsiErrorElement::class.java).toList()

    private fun CjFile.singleProperty(): CjProperty =
        PsiTreeUtil.findChildOfType(this, CjProperty::class.java)!!

    private fun List<PsiErrorElement>.descriptions(): String =
        joinToString { it.errorDescription }

    /** 合法的 get + set 形态：不应产生任何 parse 错误。 */
    @Test
    fun testGetAndSetAccessorsProduceNoError() {
        val source = """
            class C {
                private var store: Int64 = 0

                mut prop value: Int64 {
                    get() { store }
                    set(v) { store = v }
                }
            }
        """.trimIndent()

        val file = parse(source)
        val errors = file.errors()
        assertTrue(errors.isEmpty(), "合法 get/set 不应产生 parse 错误，实际：${errors.descriptions()}")

        val property = file.singleProperty()
        assertEquals(2, property.accessors.size)
        assertTrue(property.accessors[0].isGetter)
        assertTrue(property.accessors[1].isSetter)
    }

    /** 重复 getter：报一条诊断，但两个 getter 节点都必须保留。 */
    @Test
    fun testDuplicatedGetterReportsDiagnosticAndKeepsBothAccessors() {
        val source = """
            class C {
                private var store: Int64 = 0

                prop p: Int64 {
                    get() { store }
                    get() { store }
                }
            }
        """.trimIndent()

        val file = parse(source)
        val errors = file.errors()
        assertEquals(1, errors.size, "重复 getter 应只报一条诊断，实际：${errors.descriptions()}")
        assertEquals("duplicated 'get' in prop", errors.single().errorDescription)

        val property = file.singleProperty()
        assertEquals(2, property.accessors.size, "重复的 getter 节点不能丢失")
        assertTrue(property.accessors.all { it.isGetter })
        assertSame(property.accessors[0], property.getter, "getter 仍取源码顺序的第一个")

        // 官方 JSON 的 Location 指向第二个 get 关键字；仓库的 PsiErrorElement 是零宽节点，
        // 落点在关键字消费之后（同一行、关键字末尾），因此断言“重复 accessor 行内的关键字范围”。
        val firstGet = source.indexOf("get(")
        val secondGet = source.indexOf("get(", firstGet + 1)
        val error = errors.single()
        assertTrue(
            error.textRange.startOffset in secondGet..(secondGet + "get".length),
            "诊断应落在重复 get 关键字上，实际 offset=${error.textRange.startOffset}，" +
                "关键字范围=$secondGet..${secondGet + "get".length}",
        )
        assertSame(
            property.accessors[1],
            PsiTreeUtil.getParentOfType(error, CjPropertyAccessor::class.java),
            "诊断应落在重复 accessor 节点内，而不是前一个 accessor 或属性体上",
        )
    }

    /** 重复 setter：同样只报诊断，不丢节点。 */
    @Test
    fun testDuplicatedSetterReportsDiagnosticAndKeepsBothAccessors() {
        val source = """
            class C {
                private var store: Int64 = 0

                mut prop p: Int64 {
                    get() { store }
                    set(v) { store = v }
                    set(v) { store = v }
                }
            }
        """.trimIndent()

        val file = parse(source)
        val errors = file.errors()
        assertEquals(1, errors.size, "重复 setter 应只报一条诊断，实际：${errors.descriptions()}")
        assertEquals("duplicated 'set' in prop", errors.single().errorDescription)

        val property = file.singleProperty()
        assertEquals(3, property.accessors.size, "重复的 setter 节点不能丢失")
        assertTrue(property.accessors[1].isSetter)
        assertTrue(property.accessors[2].isSetter)
        assertSame(property.accessors[1], property.setter, "setter 仍取源码顺序的第一个")

        val firstSet = source.indexOf("set(")
        val secondSet = source.indexOf("set(", firstSet + 1)
        val error = errors.single()
        assertTrue(
            error.textRange.startOffset in secondSet..(secondSet + "set".length),
            "诊断应落在重复 set 关键字上，实际 offset=${error.textRange.startOffset}，" +
                "关键字范围=$secondSet..${secondSet + "set".length}",
        )
        assertSame(
            property.accessors[2],
            PsiTreeUtil.getParentOfType(error, CjPropertyAccessor::class.java),
            "诊断应落在重复 accessor 节点内，而不是前一个 accessor 或属性体上",
        )
    }

    /** 三连 getter：每个多余 accessor 各报一条。 */
    @Test
    fun testTripleGetterReportsOneDiagnosticPerDuplicate() {
        val source = """
            class C {
                private var store: Int64 = 0

                prop p: Int64 {
                    get() { store }
                    get() { store }
                    get() { store }
                }
            }
        """.trimIndent()

        val file = parse(source)
        val errors = file.errors()
        assertEquals(2, errors.size, "三连 getter 应报两条诊断，实际：${errors.descriptions()}")
        assertTrue(errors.all { it.errorDescription == "duplicated 'get' in prop" })

        val property = file.singleProperty()
        assertEquals(3, property.accessors.size)
        assertTrue(property.accessors.all { it.isGetter })
        assertSame(property.accessors[0], property.getter)
    }

    /** get / set / get：只有第三个 get 与前一个 get 重复。 */
    @Test
    fun testGetSetGetReportsOnlyRepeatedKind() {
        val source = """
            class C {
                private var store: Int64 = 0

                mut prop p: Int64 {
                    get() { store }
                    set(v) { store = v }
                    get() { store }
                }
            }
        """.trimIndent()

        val file = parse(source)
        val errors = file.errors()
        assertEquals(1, errors.size, "只有第三个 get 重复，实际：${errors.descriptions()}")
        assertEquals("duplicated 'get' in prop", errors.single().errorDescription)

        val property = file.singleProperty()
        assertEquals(3, property.accessors.size)
        assertTrue(property.accessors[0].isGetter)
        assertTrue(property.accessors[1].isSetter)
        assertTrue(property.accessors[2].isGetter)
    }
}
