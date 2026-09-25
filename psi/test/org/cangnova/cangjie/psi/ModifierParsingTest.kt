package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * modifier 序列的 parser 结构回归测试。
 *
 * `const` 同时是变量声明关键字和函数/构造器修饰符；测试必须同时锁定
 * modifier 顺序无关性与声明边界，防止函数被错建成字段或吞掉合法 const 变量。
 */
class ModifierParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    /** 初始化轻量 PSI 测试环境。 */
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    /** 释放轻量 PSI 测试环境。 */
    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * `const` 与 `override` 的相对位置不得改变 operator 函数的声明结构。
     */
    @Test
    fun testConstAndOverrideOrderKeepsOperatorFunctions() {
        val file = createPsiFile(
            "constOperatorModifierOrder",
            """
            class PermissionAnd {
                public let lhs: Int64
                public let rhs: Int64

                public operator const override func &(rhs: PermissionAnd): PermissionAnd {
                    return this
                }

                const public override operator func |(rhs: PermissionAnd): PermissionAnd {
                    return this
                }

                const override func merge(rhs: PermissionAnd): PermissionAnd {
                    return this
                }
            }
            """.trimIndent(),
        ) as CjFile

        assertNoParseErrors(file)

        val functions = PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java).toList()
        val fields = PsiTreeUtil.findChildrenOfType(file, CjFieldVariable::class.java).toList()

        assertEquals(listOf("&", "|", "merge"), functions.map { it.nameIdentifier?.text })
        assertEquals(listOf("lhs", "rhs"), fields.map { it.name })
        assertTrue(functions.take(2).all { function ->
            function.isOperator &&
                    function.isConst &&
                    function.hasModifier(CjTokens.OVERRIDE_KEYWORD)
        })
        assertTrue(functions.last().isConst)
        assertTrue(functions.last().hasModifier(CjTokens.OVERRIDE_KEYWORD))
    }

    /**
     * `const name` 仍必须进入变量/字段声明分支，不能被 modifier 前瞻吞掉。
     */
    @Test
    fun testConstVariablesAndFieldsRemainDeclarations() {
        val file = createPsiFile(
            "constVariableBoundary",
            """
            const TOP_LEVEL: Int64 = 1

            class Holder {
                public static const value: Int64 = 2
            }
            """.trimIndent(),
        ) as CjFile

        assertNoParseErrors(file)

        val topLevelVariable = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java).single()
        val field = PsiTreeUtil.findChildrenOfType(file, CjFieldVariable::class.java).single()

        assertTrue(topLevelVariable.isConst)
        assertEquals("TOP_LEVEL", topLevelVariable.pattern?.text)
        assertTrue(field.isConst)
        assertEquals("value", field.name)
    }

    /**
     * 主构造器前的 `const` 应跨越后续 modifier 与类名关联，而不是伪造字段。
     */
    @Test
    fun testConstPrimaryConstructorRemainsConstructorAfterModifierReordering() {
        val file = createPsiFile(
            "constPrimaryConstructorModifierOrder",
            """
            class Holder {
                const public Holder(value: Int64) {}
            }
            """.trimIndent(),
        ) as CjFile

        assertNoParseErrors(file)

        val constructor = PsiTreeUtil.findChildrenOfType(file, CjPrimaryConstructor::class.java).single()
        assertEquals("Holder", constructor.name)
        assertTrue(constructor.hasModifier(CjTokens.CONST_KEYWORD))
        assertTrue(constructor.hasModifier(CjTokens.PUBLIC_KEYWORD))
        assertTrue(PsiTreeUtil.findChildrenOfType(file, CjFieldVariable::class.java).isEmpty())
    }

    /**
     * `common`/`specific` 上下文关键字（官方 Lexer.cpp `GetContextualKeyword`）：修饰符位置以修饰符身份进
     * MODIFIER_LIST；解析与语言版本无关（1.0.5 与 1.1 语义下 PSI 树结构相同）。
     */
    @Test
    fun testCommonSpecificModifiersParseWithoutErrors() {
        val file = createPsiFile(
            "commonSpecificModifiers",
            """
            common class CommonThing {
                common func f(): Unit {}
            }

            specific interface SpecificThing {
                func g(): Unit
            }

            common extend CommonThing <: Object {}
            """.trimIndent(),
        ) as CjFile

        assertNoParseErrors(file)

        val cls = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java).single()
        assertTrue(cls.hasModifier(CjTokens.COMMON_KEYWORD))
        val iface = PsiTreeUtil.findChildrenOfType(file, CjInterface::class.java).single()
        assertTrue(iface.hasModifier(CjTokens.SPECIFIC_KEYWORD))
        val extend = PsiTreeUtil.findChildrenOfType(file, CjExtend::class.java).single()
        assertTrue(extend.hasModifier(CjTokens.COMMON_KEYWORD))
        // 文件内有两个命名函数（class 成员 f 与 interface 成员 g），按名选取断言
        val classMember = PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java)
            .single { it.name == "f" }
        assertTrue(classMember.hasModifier(CjTokens.COMMON_KEYWORD))
        val interfaceMember = PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java)
            .single { it.name == "g" }
        assertTrue(!interfaceMember.hasModifier(CjTokens.COMMON_KEYWORD))
    }

    /**
     * `common`/`specific` 作 import 包名段（官方 `ExpectPackageIdentWithPos` 接受上下文关键字；
     * 标准库 `std.unittest.common` 即此形态）：导入全名须完整，后续 import 不受影响。
     */
    @Test
    fun testCjmpKeywordAsImportPackageSegment() {
        val file = createPsiFile(
            "cjmpKeywordImport",
            """
            import std.unittest.common.*
            import std.unittest.diff.*
            import a.specific.B
            import std.unittest.{common.*, diff.*}
            """.trimIndent(),
        ) as CjFile

        assertNoParseErrors(file)
        val imported = PsiTreeUtil.findChildrenOfType(file, CjImportItem::class.java)
            .mapNotNull { it.importedFqName?.asString() }
        assertTrue("std.unittest.common" in imported, "keyword segment kept in import path: $imported")
        assertTrue("std.unittest.diff" in imported, "following import unaffected: $imported")
        assertTrue("a.specific.B" in imported, "specific segment kept in import path: $imported")
        assertEquals(5, imported.size, "multi-import items keep keyword segments: $imported")
    }

    /**
     * 非修饰符位置的 `common`/`specific` 是普通标识符：cjc 1.0.5 与 1.1.3（含 `--experimental`）
     * 均接受包名、变量、函数、形参、成员、类型名、枚举构造器与泛型形参取这两个名字。
     */
    @Test
    fun testCommonSpecificRemainIdentifiersOutsideModifierPosition() {
        val file = createPsiFile(
            "cjmpKeywordIdentifiers",
            """
            package a.common.specific

            let common: Int64 = 1
            var specific: Int64 = 2

            func common(specific: Int64): Int64 { specific }

            class Holder {
                var common: Int64 = 1
                func specific(): Int64 { common }
            }

            struct specific {}

            enum E {
                | common | specific
            }

            func g<common>(x: common): common { x }

            main(): Int64 {
                let common = 3
                var specific = 4
                specific = common + specific
                let h = Holder()
                h.common = h.specific()
                match (E.common) {
                    case common => 1
                    case specific => 2
                }
            }
            """.trimIndent(),
        ) as CjFile

        assertNoParseErrors(file)
        assertEquals("a.common.specific", file.packageFqName.asString())

        val functionNames = PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java).map { it.name }
        assertTrue(functionNames.containsAll(listOf("common", "specific", "g")), "function names: $functionNames")
        val fields = PsiTreeUtil.findChildrenOfType(file, CjFieldVariable::class.java).map { it.name }
        assertEquals(listOf("common"), fields)
        val structName = PsiTreeUtil.findChildrenOfType(file, CjStruct::class.java).single().name
        assertEquals("specific", structName)
        val enumEntries = PsiTreeUtil.findChildrenOfType(file, CjEnumConstructor::class.java).map { it.name }
        assertEquals(listOf("common", "specific"), enumEntries)
        val typeParameter = PsiTreeUtil.findChildrenOfType(file, CjTypeParameter::class.java).single()
        assertEquals("common", typeParameter.name)
        // 标识符位置的 common/specific 不得被误收为修饰符
        val cjmpModifiers = PsiTreeUtil.findChildrenOfType(file, CjModifierListOwner::class.java)
            .filter { it.hasModifier(CjTokens.COMMON_KEYWORD) || it.hasModifier(CjTokens.SPECIFIC_KEYWORD) }
        assertTrue(cjmpModifiers.isEmpty(), "unexpected cjmp modifiers on: ${cjmpModifiers.map { it.text }}")
    }

    /** 断言源码不包含 parser 错误节点。 */
    private fun assertNoParseErrors(file: CjFile) {
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(
            errors.isEmpty(),
            "source should parse without PsiErrorElement, but got: ${
                errors.joinToString { error ->
                    val offset = error.textRange.startOffset
                    val context = file.text.substring(maxOf(0, offset - 20), minOf(file.text.length, offset + 20))
                    "${error.errorDescription} @$offset[${context.replace('\n', '⏎')}]"
                }
            }",
        )
    }
}
