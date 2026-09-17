package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @! 参数注解不得吞掉主构造成员关键字、参数名、命名标记及默认值。
 * 官方 ParseDecl.cpp 的 ParseParamInParamList 先解析注解，再解析完整参数声明。
 */
class CompileTimeVisibleParameterParsingTest : CjParsingTestCase(
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

    @Test
    fun primaryConstructorParameterKeepsItsMemberAndDefaultSyntax() {
        val file = parseSource()
        val holder = file.declarations.filterIsInstance<CjClass>().single()
        val constructor = checkNotNull(holder.primaryConstructor)
        val parameter = constructor.valueParameters.single()
        assertEquals("v", parameter.name)
        assertEquals("Int64", parameter.typeReference?.text)
        assertTrue(parameter.hasLetOrVar())
        assertTrue(parameter.isMutable)
        assertEquals(CjTokens.VAR_KEYWORD, parameter.letOrVarKeyword?.node?.elementType)
        assertTrue(parameter.hasModifier(CjTokens.PUBLIC_KEYWORD))
        assertTrue(parameter.isNamed)
        assertTrue(parameter.hasDefaultValue())
        assertEquals("1", parameter.defaultValue?.text)
        assertAnnotation(parameter, "1")
        assertTrue(constructor.annotationEntries.isEmpty())
    }

    @Test
    fun ordinaryParametersKeepPositionalAndNamedFormsAfterCompileTimeAnnotations() {
        val file = parseSource()
        val function = file.declarations.filterIsInstance<CjNamedFunction>().single()
        val parameters = function.valueParameters
        assertEquals(listOf("value", "other"), parameters.map { it.name })
        assertEquals(listOf(false, true), parameters.map { it.isNamed })
        assertEquals(listOf(false, true), parameters.map { it.hasDefaultValue() })
        assertEquals(listOf(null, "2"), parameters.map { it.defaultValue?.text })
        for (parameter in parameters) {
            assertFalse(parameter.hasLetOrVar())
            assertEquals("Int64", parameter.typeReference?.text)
        }
        assertAnnotation(parameters[0], "2")
        assertAnnotation(parameters[1], "3")
        assertTrue(function.annotationEntries.isEmpty())
    }

    private fun assertAnnotation(parameter: CjParameter, argumentText: String) {
        val annotation = parameter.annotationEntries.single()
        assertEquals("ParamTag", annotation.shortName?.asString())
        assertEquals("@!ParamTag[$argumentText]", annotation.text)
        assertTrue(annotation.isCompileTimeVisible)
        assertEquals(argumentText, annotation.valueArguments.single().getArgumentExpression()?.text)
        assertTrue(PsiTreeUtil.findChildrenOfType(parameter, CjMacroExpression::class.java).isEmpty())
    }

    private fun parseSource(): CjFile {
        val file = createPsiFile("compileTimeParameters", SOURCE) as CjFile
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(errors.isEmpty(), errors.joinToString { it.errorDescription })
        return file
    }

    companion object {
        private val SOURCE = """
            class Holder {
                public Holder(@!ParamTag[1] public var v!: Int64 = 1) {}
            }

            func ordinary(@!ParamTag[2] value: Int64, @!ParamTag[3] other!: Int64 = 2): Int64 {
                value + other
            }
        """.trimIndent()
    }
}
