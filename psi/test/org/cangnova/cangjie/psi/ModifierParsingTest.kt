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

    /** 断言源码不包含 parser 错误节点。 */
    private fun assertNoParseErrors(file: CjFile) {
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(
            errors.isEmpty(),
            "source should parse without PsiErrorElement, but got: ${
                errors.joinToString { it.errorDescription }
            }",
        )
    }
}
