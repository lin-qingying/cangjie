package org.cangnova.cangjie.codeinsight.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            CangJieHighlightingColors.FUNCTION_DECLARATION,
            CangJieHighlightInfoTypeSemanticNames.FUNCTION_DECLARATION.attributesKey,
        )
        assertEquals(
            CangJieHighlightingColors.MACRO_CALL,
            CangJieHighlightInfoTypeSemanticNames.MACRO_CALL.attributesKey,
        )
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
        CangJieHighlightingColors.INVALID_STRING_ESCAPE,
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
        CangJieHighlightingColors.PARAMETER,
        CangJieHighlightingColors.INSTANCE_PROPERTY,
        CangJieHighlightingColors.INSTANCE_VARIABLE,
        CangJieHighlightingColors.WRAPPED_INTO_REF,
        CangJieHighlightingColors.BACKING_FIELD_VARIABLE,
        CangJieHighlightingColors.EXTENSION_PROPERTY,
        CangJieHighlightingColors.FUNCTION_DECLARATION,
        CangJieHighlightingColors.FUNCTION_CALL,
        CangJieHighlightingColors.PACKAGE_FUNCTION_CALL,
        CangJieHighlightingColors.EXTENSION_FUNCTION_CALL,
        CangJieHighlightingColors.CONSTRUCTOR_CALL,
        CangJieHighlightingColors.VARIABLE_AS_FUNCTION_CALL,
        CangJieHighlightingColors.VARIABLE_AS_FUNCTION_LIKE_CALL,
        CangJieHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW,
        CangJieHighlightingColors.ANNOTATION,
        CangJieHighlightingColors.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES,
        CangJieHighlightingColors.MACRO_DECLARATION,
        CangJieHighlightingColors.MACRO_CALL,
        CangJieHighlightingColors.SMART_CAST_VALUE,
        CangJieHighlightingColors.SMART_CONSTANT,
        CangJieHighlightingColors.SMART_CAST_RECEIVER,
        CangJieHighlightingColors.DEBUG_INFO,
        CangJieHighlightingColors.BAD_CHARACTER,
        CangJieHighlightingColors.LABEL,
        CangJieHighlightingColors.RESOLVED_TO_ERROR,
        CangJieHighlightingColors.NAMED_ARGUMENT,
        CangJieHighlightingColors.LT_COLON,
        CangJieHighlightingColors.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
        CangJieHighlightingColors.DOUBLE_COLON,
        CangJieHighlightingColors.EXCLEXCL,
    )
}
