/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.codeinsight.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

/**
 * 仓颉高亮项的共享 TextAttributesKey 目录。
 *
 * code-insight 以 IntelliJ Platform 的高亮框架作为共享语言服务框架：
 * 这里定义“仓颉有哪些高亮项”和它们的默认 fallback，不定义具体主题颜色，也不定义具体使用方的展示入口。
 */
object CangJieHighlightingColors {
    val KEYWORD: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

    val LET_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_LET", KEYWORD)
    val MUT_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_MUT", KEYWORD)
    val PROP_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_PROP", KEYWORD)
    val VAR_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_VAR", KEYWORD)
    val CONST_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_CONST", KEYWORD)
    val QUOTE_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_QUOTE", KEYWORD)
    val STATIC_KEYWORD: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_STATIC_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

    val LINE_COMMENT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    val BLOCK_COMMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_BLOCK_COMMENT",
        DefaultLanguageHighlighterColors.BLOCK_COMMENT,
    )
    val DOC_COMMENT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT)
    val CDOC_TAG: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CDOC_TAG_NAME", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)
    val CDOC_LINK: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CDOC_LINK", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE)

    val NUMBER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    val STRING: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_STRING", DefaultLanguageHighlighterColors.STRING)
    val STRING_ESCAPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_STRING_ESCAPE",
        DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE,
    )
    val INVALID_STRING_ESCAPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INVALID_STRING_ESCAPE",
        DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE,
    )

    val OPERATOR_SIGN: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_OPERATION_SIGN",
        DefaultLanguageHighlighterColors.OPERATION_SIGN,
    )
    val PARENTHESIS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_PARENTHESIS", DefaultLanguageHighlighterColors.PARENTHESES)
    val BRACES: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BRACES", DefaultLanguageHighlighterColors.BRACES)
    val BRACKETS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
    val COMMA: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_COMMA", DefaultLanguageHighlighterColors.COMMA)
    val SEMICOLON: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
    val COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_COLON")
    val DOT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_DOT", DefaultLanguageHighlighterColors.DOT)
    val SAFE_ACCESS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SAFE_ACCESS", DefaultLanguageHighlighterColors.DOT)
    val QUEST: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_QUEST")
    val ARROW: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_ARROW", PARENTHESIS)
    val DOUBLE_ARROW: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_DOUBLE_ARROW", PARENTHESIS)

    val TYPE_DEFINED: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_DEFINED", DefaultLanguageHighlighterColors.CLASS_NAME)
    val TYPE_REFERENCE: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_REFERENCE", DefaultLanguageHighlighterColors.CLASS_NAME)
    val CLASS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_CLASS", TYPE_DEFINED)
    val TYPE_PARAMETER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER)
    val ABSTRACT_CLASS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ABSTRACT_CLASS", TYPE_DEFINED)
    val INTERFACE: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_INTERFACE", TYPE_DEFINED)
    val STRUCT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_STRUCT", TYPE_DEFINED)
    val ENUM: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ENUM", TYPE_DEFINED)
    val ENUM_CONSTRUCTOR: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ENUM_CONSTRUCTOR", DefaultLanguageHighlighterColors.STATIC_FIELD)
    val TYPE_ALIAS: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_ALIAS", TYPE_DEFINED)

    val MUTABLE_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_MUTABLE_VARIABLE")
    val MUTABLE_PROPERTY: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_MUTABLE_PROPERTY", MUTABLE_VARIABLE)
    val LOCAL_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_LOCAL_VARIABLE",
        DefaultLanguageHighlighterColors.LOCAL_VARIABLE,
    )
    val PROPERTY: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_PROPERTY", LOCAL_VARIABLE)
    val PACKAGE_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_PACKAGE_VARIABLE",
        DefaultLanguageHighlighterColors.STATIC_FIELD,
    )
    val PARAMETER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER)
    val INSTANCE_PROPERTY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INSTANCE_PROPERTY",
        DefaultLanguageHighlighterColors.INSTANCE_FIELD,
    )
    val INSTANCE_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INSTANCE_VARIABLE",
        DefaultLanguageHighlighterColors.INSTANCE_FIELD,
    )
    val WRAPPED_INTO_REF: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_WRAPPED_INTO_REF",
        DefaultLanguageHighlighterColors.CLASS_NAME,
    )
    val BACKING_FIELD_VARIABLE: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BACKING_FIELD_VARIABLE")
    val EXTENSION_PROPERTY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_EXTENSION_PROPERTY",
        DefaultLanguageHighlighterColors.STATIC_FIELD,
    )

    val FUNCTION_DECLARATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_FUNCTION_DECLARATION",
        DefaultLanguageHighlighterColors.FUNCTION_DECLARATION,
    )
    val FUNCTION_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_FUNCTION_CALL",
        DefaultLanguageHighlighterColors.FUNCTION_CALL,
    )
    val PACKAGE_FUNCTION_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_PACKAGE_FUNCTION_CALL",
        DefaultLanguageHighlighterColors.STATIC_METHOD,
    )
    val EXTENSION_FUNCTION_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_EXTENSION_FUNCTION_CALL",
        DefaultLanguageHighlighterColors.STATIC_METHOD,
    )
    val CONSTRUCTOR_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_CONSTRUCTOR", DefaultLanguageHighlighterColors.FUNCTION_CALL)
    val VARIABLE_AS_FUNCTION_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_VARIABLE_AS_FUNCTION")
    val VARIABLE_AS_FUNCTION_LIKE_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_VARIABLE_AS_FUNCTION_LIKE")
    val FUNCTION_LITERAL_BRACES_AND_ARROW: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_FUNCTION_LITERAL_BRACES_AND_ARROW")

    val ANNOTATION: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ANNOTATION", DefaultLanguageHighlighterColors.METADATA)
    val ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES",
        DefaultLanguageHighlighterColors.METADATA,
    )

    val MACRO_DECLARATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_MACRO_DECLARATION")
    val MACRO_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_MACRO_CALL")

    val SMART_CAST_VALUE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_SMART_CAST_VALUE")
    val SMART_CONSTANT: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_SMART_CONSTANT")
    val SMART_CAST_RECEIVER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SMART_CAST_RECEIVER")
    val DEBUG_INFO: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_DEBUG_INFO")

    val BAD_CHARACTER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
    val LABEL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_LABEL", DefaultLanguageHighlighterColors.LABEL)
    val RESOLVED_TO_ERROR: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_RESOLVED_TO_ERROR")
    val NAMED_ARGUMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_NAMED_ARGUMENT")
    val LT_COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_LT_COLON")
    val INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION",
        INSTANCE_PROPERTY,
    )
    val DOUBLE_COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_DOUBLE_COLON")
    val EXCLEXCL: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_EXCLEXCL")
}
