package org.cangnova.cangjie.codeinsight.highlighting

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjInterface
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjStruct
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class CangJieHighlightingStructuralRulesTest : CjParsingTestCase(
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
    fun testClassStructInterfaceTypeAliasAndTypeParameterRules() {
        val file = parse(
            """
            class Box<T> {}
            struct Point {}
            interface Shape {}
            type Alias = Box<Int64>
            """.trimIndent(),
        )

        assertEquals(
            CangJieHighlightInfoTypeSemanticNames.CLASS,
            CangJieHighlightingStructuralRules.highlightInfoTypeForClass(file.singleDescendant<CjClass>()),
        )
        assertEquals(
            CangJieHighlightInfoTypeSemanticNames.STRUCT,
            CangJieHighlightingStructuralRules.highlightInfoTypeForClass(file.singleDescendant<CjStruct>()),
        )
        assertEquals(
            CangJieHighlightInfoTypeSemanticNames.INTERFACE,
            CangJieHighlightingStructuralRules.highlightInfoTypeForClass(file.singleDescendant<CjInterface>()),
        )
        assertEquals(
            CangJieHighlightInfoTypeSemanticNames.TYPE_ALIAS,
            CangJieHighlightingStructuralRules.highlightInfoTypeForTypeDeclaration(file.singleDescendant<CjTypeAlias>()),
        )
        assertEquals(
            CangJieHighlightInfoTypeSemanticNames.TYPE_PARAMETER,
            CangJieHighlightingStructuralRules.highlightInfoTypeForTypeDeclaration(file.singleDescendant<CjTypeParameter>()),
        )
    }

    @Test
    fun testFunctionPropertyParameterAndVariableRules() {
        val file = parse(
            """
            class Box {
                let member: Int64 = 0
                func value(param: Int64): Int64 {
                    let local = param
                    return local
                }
            }
            """.trimIndent(),
        )

        assertEquals(
            CangJieHighlightInfoTypeSemanticNames.FUNCTION_DECLARATION,
            CangJieHighlightingStructuralRules.highlightInfoTypeForFunction(file.singleDescendant<CjNamedFunction>()),
        )
        assertEquals(
            CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY,
            CangJieHighlightingStructuralRules.highlightInfoTypeForPropertyDeclaration(file.singleDescendant<CjFieldVariable>()),
        )
        assertEquals(
            CangJieHighlightInfoTypeSemanticNames.PARAMETER,
            CangJieHighlightingStructuralRules.highlightInfoTypeForParameterDeclaration(file.singleDescendant<CjParameter>()),
        )
        assertEquals(
            CangJieHighlightInfoTypeSemanticNames.LOCAL_VARIABLE,
            CangJieHighlightingStructuralRules.highlightInfoTypeForVariableDeclaration(file.singleDescendant<CjPatternVariable>()),
        )
    }

    @Test
    fun testCompatibilityTopLevelFunctionsDelegateToSharedRules() {
        val file = parse(
            """
            class Box {
                let member: Int64 = 0
            }
            func value(param: Int64): Int64 {
                let local = param
                return local
            }
            """.trimIndent(),
        )

        val cclass = file.singleDescendant<CjClass>()
        val field = file.singleDescendant<CjFieldVariable>()
        val function = file.singleDescendant<CjNamedFunction>()
        val parameter = file.singleDescendant<CjParameter>()
        val variable = file.singleDescendant<CjPatternVariable>()

        assertEquals(CangJieHighlightInfoTypeSemanticNames.CLASS, textAttributesForClass(cclass))
        assertEquals(CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY, textAttributesKeyForPropertyDeclaration(field))
        assertEquals(CangJieHighlightInfoTypeSemanticNames.FUNCTION_DECLARATION, textAttributesKeyForCjFunction(function))
        assertEquals(CangJieHighlightInfoTypeSemanticNames.PARAMETER, textAttributesForCjParameterDeclaration(parameter))
        assertEquals(CangJieHighlightInfoTypeSemanticNames.LOCAL_VARIABLE, textAttributesForCjVariableDeclaration(variable))
    }

    private fun parse(text: String): CjFile =
        createPsiFile("highlighting", text) as CjFile

    private inline fun <reified T : org.cangnova.cangjie.psi.CjElement> CjFile.singleDescendant(): T {
        val element = collectDescendantsOfType<T>().singleOrNull()
        assertNotNull(element, "Expected exactly one ${T::class.simpleName} in test file")
        return element
    }
}
