package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * 表示 `OptionalSyntaxParsingTest`，承载PSI 测试中的语法节点、索引桩或辅助模型。
 */
class OptionalSyntaxParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    /**
     * 提供 `setUpFixture` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    /**
     * 提供 `tearDownFixture` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * 提供 `testOptionTypeParsesAsNestedOptionalType` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
    @Test
    fun testOptionTypeParsesAsNestedOptionalType() {
        val file = createPsiFile("optionalType", "func sample(value: ??Int64) {}") as CjFile
        val optionTypes = PsiTreeUtil.findChildrenOfType(file, CjOptionType::class.java)
        assertEquals(2, optionTypes.size)
    }

    /**
     * 提供 `testOptionalChainBuildsOptionalPsiNodes` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
    @Test
    fun testOptionalChainBuildsOptionalPsiNodes() {
        val file = createPsiFile(
            "optionalChain",
            """
            func sample() {
                x?.b?[1]?()
            }
            """.trimIndent(),
        ) as CjFile

        assertNotNull(PsiTreeUtil.findChildOfType(file, CjOptionalExpression::class.java))
        assertNotNull(PsiTreeUtil.findChildOfType(file, CjOptionalChainExpression::class.java))
    }

    /**
     * 提供 `testInvalidStandaloneQuestReportsError` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
    @Test
    fun testInvalidStandaloneQuestReportsError() {
        val file = createPsiFile(
            "invalidQuest",
            """
            func sample() {
                x?b
            }
            """.trimIndent(),
        ) as CjFile

        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(errors.isNotEmpty())
    }

    /**
     * 提供 `testAbstractFunctionWithoutBodyDoesNotProduceParseError` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
    @Test
    fun testAbstractFunctionWithoutBodyDoesNotProduceParseError() {
        val file = createPsiFile(
            "abstractFunctionWithoutBody",
            """
            class AstNode {
                abstract func dump(indent: UInt16): std.core.String
            }
            """.trimIndent(),
        ) as CjFile

        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(
            errors.isEmpty(),
            "abstract function without body should parse without PsiErrorElement, but got: ${
                errors.joinToString { it.errorDescription }
            }",
        )
    }

    /**
     * prop set 参数只需名字，类型由属性类型决定（对齐官方 PROP_MEMBER_SETTER_BODY）。
     */
    @Test
    fun testPropSetterParameterDoesNotRequireType() {
        val file = createPsiFile(
            "propSetterNoType",
            """
            class C {
                mut prop value: Int64 {
                    get() { 0 }
                    set(v) { }
                }
            }
            """.trimIndent(),
        ) as CjFile

        val setter = PsiTreeUtil.findChildrenOfType(file, CjPropertyAccessor::class.java)
            .single { it.isSetter }
        val parameter = setter.valueParameters.single()
        assertEquals("v", parameter.name)
        assertEquals(null, parameter.typeReference)

        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(
            errors.isEmpty(),
            "prop setter parameter without type should parse without PsiErrorElement, but got: ${
                errors.joinToString { it.errorDescription }
            }",
        )
    }

    /**
     * 普通函数参数仍必须写类型。
     */
    @Test
    fun testFunctionParameterStillRequiresType() {
        val file = createPsiFile(
            "functionParamRequiresType",
            """
            func sample(x) {}
            """.trimIndent(),
        ) as CjFile

        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(
            errors.any { it.errorDescription.contains("type", ignoreCase = true) },
            "function parameter without type should report a type-related parse error, but got: ${
                errors.joinToString { it.errorDescription }
            }",
        )
    }
}
