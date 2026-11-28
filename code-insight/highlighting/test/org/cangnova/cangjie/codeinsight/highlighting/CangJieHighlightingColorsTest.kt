package org.cangnova.cangjie.codeinsight.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.cangnova.cangjie.lexer.CjTokens

/**
 * 验证仓颉高亮属性 key 与语义 HighlightInfoType 的稳定性。
 */
class CangJieHighlightingColorsTest {
    /**
     * 所有 TextAttributesKey 外部名必须稳定且唯一。
     */
    @Test
    fun testTextAttributeKeyExternalNamesAreStableAndUnique() {
        val names = allKeys.map(TextAttributesKey::getExternalName)

        assertEquals(names.size, names.toSet().size)
        assertTrue(names.all { name -> name.isNotBlank() })
        assertTrue("CANGJIE_KEYWORD" in names)
        assertTrue("CANGJIE_CLASS" in names)
        assertTrue("CANGJIE_FUNCTION_DECLARATION" in names)
        assertTrue("CANGJIE_MACRO_CALL" in names)
        assertTrue("CANGJIE_BAD_CHARACTER" in names)
    }

    /**
     * 语义 HighlightInfoType 必须复用共享 TextAttributesKey。
     */
    @Test
    fun testSemanticHighlightInfoTypesUseSharedAttributeKeys() {
        assertEquals(
            CangJieHighlightingColors.CLASS,
            CangJieHighlightInfoTypeSemanticNames.CLASS.attributesKey,
        )
        assertEquals(
            CangJieHighlightingColors.STRUCT,
            CangJieHighlightInfoTypeSemanticNames.STRUCT.attributesKey,
        )
        assertEquals(
            CangJieHighlightingColors.BASIC_TYPE_REFERENCE,
            CangJieHighlightInfoTypeSemanticNames.BASIC_TYPE_REFERENCE.attributesKey,
        )
        assertEquals(
            CangJieHighlightingColors.STATIC_FUNCTION_CALL,
            CangJieHighlightInfoTypeSemanticNames.STATIC_FUNCTION_CALL.attributesKey,
        )
        assertEquals(
            CangJieHighlightingColors.ANNOTATION,
            CangJieHighlightInfoTypeSemanticNames.ANNOTATION.attributesKey,
        )
        assertEquals(
            CangJieHighlightingColors.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES,
            CangJieHighlightInfoTypeSemanticNames.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES.attributesKey,
        )
        assertEquals(
            CangJieHighlightingColors.STRUCT_CONSTRUCTOR_CALL,
            CangJieHighlightInfoTypeSemanticNames.STRUCT_CONSTRUCTOR_CALL.attributesKey,
        )
        assertEquals(
            CangJieHighlightingColors.FUNCTION_DECLARATION,
            CangJieHighlightInfoTypeSemanticNames.FUNCTION_DECLARATION.attributesKey,
        )
        assertEquals(
            CangJieHighlightingColors.MACRO_CALL,
            CangJieHighlightInfoTypeSemanticNames.MACRO_CALL.attributesKey,
        )
        assertEquals(
            CangJieHighlightingColors.MACRO_DECLARATION,
            CangJieHighlightInfoTypeSemanticNames.MACRO_DECLARATION.attributesKey,
        )
        assertEquals(
            CangJieHighlightingColors.MACRO_ESCAPE,
            CangJieHighlightInfoTypeSemanticNames.MACRO_ESCAPE.attributesKey,
        )
        assertEquals(
            CangJieHighlightingColors.SAFE_ACCESS,
            CangJieHighlightInfoTypeSemanticNames.SAFE_ACCESS.attributesKey,
        )
    }

    @Test
    fun testBasicTypeReferencesHaveTheirOwnSemanticKind() {
        assertEquals(
            CangJieSemanticHighlightingKind.BASIC_TYPE_REFERENCE,
            CangJieSemanticHighlightingRules.classifyBasicType().kind,
        )
    }

    @Test
    fun testSafeAccessQuestionMarkUsesOnlyOptionalChainSuffixes() {
        listOf(CjTokens.DOT, CjTokens.LBRACKET, CjTokens.LPAR, CjTokens.LBRACE).forEach { suffix ->
            assertTrue(CangJieSemanticHighlightingRules.isSafeAccessQuestionMark(CjTokens.QUEST, suffix))
        }

        assertFalse(CangJieSemanticHighlightingRules.isSafeAccessQuestionMark(CjTokens.QUEST, CjTokens.IDENTIFIER))
        assertFalse(CangJieSemanticHighlightingRules.isSafeAccessQuestionMark(CjTokens.PLUS, CjTokens.DOT))
    }

    /**
     * 测试覆盖的完整仓颉 TextAttributesKey 列表。
     */
    private val allKeys: List<TextAttributesKey> = listOf(
        CangJieHighlightingColors.KEYWORD,
        CangJieHighlightingColors.LET_KEYWORD,
        CangJieHighlightingColors.MUT_KEYWORD,
        CangJieHighlightingColors.PROP_KEYWORD,
        CangJieHighlightingColors.VAR_KEYWORD,
        CangJieHighlightingColors.CONST_KEYWORD,
        CangJieHighlightingColors.QUOTE_KEYWORD,
        CangJieHighlightingColors.STATIC_KEYWORD,
        CangJieHighlightingColors.LINE_COMMENT,
        CangJieHighlightingColors.BLOCK_COMMENT,
        CangJieHighlightingColors.DOC_COMMENT,
        CangJieHighlightingColors.CDOC_TAG,
        CangJieHighlightingColors.CDOC_LINK,
        CangJieHighlightingColors.NUMBER,
        CangJieHighlightingColors.STRING,
        CangJieHighlightingColors.STRING_ESCAPE,
        CangJieHighlightingColors.MACRO_ESCAPE,
        CangJieHighlightingColors.OPERATOR_SIGN,
        CangJieHighlightingColors.PARENTHESIS,
        CangJieHighlightingColors.BRACES,
        CangJieHighlightingColors.BRACKETS,
        CangJieHighlightingColors.COMMA,
        CangJieHighlightingColors.SEMICOLON,
        CangJieHighlightingColors.COLON,
        CangJieHighlightingColors.DOT,
        CangJieHighlightingColors.SAFE_ACCESS,
        CangJieHighlightingColors.QUEST,
        CangJieHighlightingColors.ARROW,
        CangJieHighlightingColors.DOUBLE_ARROW,
        CangJieHighlightingColors.TYPE_DEFINED,
        CangJieHighlightingColors.TYPE_REFERENCE,
        CangJieHighlightingColors.BASIC_TYPE_REFERENCE,
        CangJieHighlightingColors.CLASS,
        CangJieHighlightingColors.TYPE_PARAMETER,
        CangJieHighlightingColors.ABSTRACT_CLASS,
        CangJieHighlightingColors.INTERFACE,
        CangJieHighlightingColors.STRUCT,
        CangJieHighlightingColors.ENUM,
        CangJieHighlightingColors.ENUM_CONSTRUCTOR,
        CangJieHighlightingColors.TYPE_ALIAS,
        CangJieHighlightingColors.MUTABLE_VARIABLE,
        CangJieHighlightingColors.MUTABLE_PROPERTY,
        CangJieHighlightingColors.LOCAL_VARIABLE,
        CangJieHighlightingColors.PROPERTY,
        CangJieHighlightingColors.PACKAGE_VARIABLE,
        CangJieHighlightingColors.STATIC_VARIABLE,
        CangJieHighlightingColors.PARAMETER,
        CangJieHighlightingColors.INSTANCE_PROPERTY,
        CangJieHighlightingColors.INSTANCE_VARIABLE,
        CangJieHighlightingColors.EXTENSION_PROPERTY,
        CangJieHighlightingColors.FUNCTION_DECLARATION,
        CangJieHighlightingColors.FUNCTION_CALL,
        CangJieHighlightingColors.INSTANCE_FUNCTION_CALL,
        CangJieHighlightingColors.STATIC_FUNCTION_CALL,
        CangJieHighlightingColors.PACKAGE_FUNCTION_CALL,
        CangJieHighlightingColors.EXTENSION_FUNCTION_CALL,
        CangJieHighlightingColors.CONSTRUCTOR_CALL,
        CangJieHighlightingColors.CLASS_CONSTRUCTOR_CALL,
        CangJieHighlightingColors.STRUCT_CONSTRUCTOR_CALL,
        CangJieHighlightingColors.VARIABLE_AS_FUNCTION_CALL,
        CangJieHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW,
        CangJieHighlightingColors.ANNOTATION,
        CangJieHighlightingColors.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES,
        CangJieHighlightingColors.MACRO_DECLARATION,
        CangJieHighlightingColors.MACRO_CALL,
        CangJieHighlightingColors.BAD_CHARACTER,
        CangJieHighlightingColors.RESOLVED_TO_ERROR,
        CangJieHighlightingColors.NAMED_ARGUMENT,
        CangJieHighlightingColors.LT_COLON,
        CangJieHighlightingColors.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
        CangJieHighlightingColors.STATIC_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
        CangJieHighlightingColors.DOUBLE_COLON,
        CangJieHighlightingColors.EXCLEXCL,
    )
}
