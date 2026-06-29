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
    /** 通用仓颉关键字高亮属性。 */
    val KEYWORD: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

    /** `let` 关键字专用高亮属性。 */
    val LET_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_LET", KEYWORD)
    /** `mut` 关键字专用高亮属性。 */
    val MUT_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_MUT", KEYWORD)
    /** `prop` 关键字专用高亮属性。 */
    val PROP_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_PROP", KEYWORD)
    /** `var` 关键字专用高亮属性。 */
    val VAR_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_VAR", KEYWORD)
    /** `const` 关键字专用高亮属性。 */
    val CONST_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_CONST", KEYWORD)
    /** `quote` 关键字专用高亮属性。 */
    val QUOTE_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_QUOTE", KEYWORD)
    /** 静态声明关键字高亮属性。 */
    val STATIC_KEYWORD: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_STATIC_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

    /** 行注释高亮属性。 */
    val LINE_COMMENT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    /** 块注释高亮属性。 */
    val BLOCK_COMMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_BLOCK_COMMENT",
        DefaultLanguageHighlighterColors.BLOCK_COMMENT,
    )
    /** 文档注释整体高亮属性。 */
    val DOC_COMMENT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT)
    /** CDoc 标签名称高亮属性。 */
    val CDOC_TAG: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CDOC_TAG_NAME", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)
    /** CDoc 链接目标高亮属性。 */
    val CDOC_LINK: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CDOC_LINK", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE)

    /** 数字字面量高亮属性。 */
    val NUMBER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    /** 字符串与 rune 字面量高亮属性。 */
    val STRING: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_STRING", DefaultLanguageHighlighterColors.STRING)
    /** 合法字符串转义序列高亮属性。 */
    val STRING_ESCAPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_STRING_ESCAPE",
        DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE,
    )
    /** 非法字符串转义序列高亮属性。 */
    val INVALID_STRING_ESCAPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INVALID_STRING_ESCAPE",
        DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE,
    )

    /** 运算符符号高亮属性。 */
    val OPERATOR_SIGN: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_OPERATION_SIGN",
        DefaultLanguageHighlighterColors.OPERATION_SIGN,
    )
    /** 圆括号高亮属性。 */
    val PARENTHESIS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_PARENTHESIS", DefaultLanguageHighlighterColors.PARENTHESES)
    /** 花括号高亮属性。 */
    val BRACES: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BRACES", DefaultLanguageHighlighterColors.BRACES)
    /** 方括号高亮属性。 */
    val BRACKETS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
    /** 逗号高亮属性。 */
    val COMMA: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_COMMA", DefaultLanguageHighlighterColors.COMMA)
    /** 分号高亮属性。 */
    val SEMICOLON: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
    /** 冒号高亮属性。 */
    val COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_COLON")
    /** 点号高亮属性。 */
    val DOT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_DOT", DefaultLanguageHighlighterColors.DOT)
    /** 安全访问运算符高亮属性。 */
    val SAFE_ACCESS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SAFE_ACCESS", DefaultLanguageHighlighterColors.DOT)
    /** 问号符号高亮属性。 */
    val QUEST: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_QUEST")
    /** 单箭头符号高亮属性。 */
    val ARROW: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_ARROW", PARENTHESIS)
    /** 双箭头符号高亮属性。 */
    val DOUBLE_ARROW: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_DOUBLE_ARROW", PARENTHESIS)

    /** 类型定义名称的基础高亮属性。 */
    val TYPE_DEFINED: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_DEFINED", DefaultLanguageHighlighterColors.CLASS_NAME)
    /** 类型引用名称高亮属性。 */
    val TYPE_REFERENCE: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_REFERENCE", DefaultLanguageHighlighterColors.CLASS_NAME)
    /** class 声明名称高亮属性。 */
    val CLASS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_CLASS", TYPE_DEFINED)
    /** 类型参数名称高亮属性。 */
    val TYPE_PARAMETER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER)
    /** 抽象 class 声明名称高亮属性。 */
    val ABSTRACT_CLASS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ABSTRACT_CLASS", TYPE_DEFINED)
    /** interface 声明名称高亮属性。 */
    val INTERFACE: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_INTERFACE", TYPE_DEFINED)
    /** struct 声明名称高亮属性。 */
    val STRUCT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_STRUCT", TYPE_DEFINED)
    /** enum 声明名称高亮属性。 */
    val ENUM: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ENUM", TYPE_DEFINED)
    /** enum 构造项高亮属性。 */
    val ENUM_CONSTRUCTOR: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ENUM_CONSTRUCTOR", DefaultLanguageHighlighterColors.STATIC_FIELD)
    /** typealias 声明名称高亮属性。 */
    val TYPE_ALIAS: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_ALIAS", TYPE_DEFINED)

    /** 可变局部变量高亮属性。 */
    val MUTABLE_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_MUTABLE_VARIABLE")
    /** 可变属性高亮属性。 */
    val MUTABLE_PROPERTY: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_MUTABLE_PROPERTY", MUTABLE_VARIABLE)
    /** 局部变量高亮属性。 */
    val LOCAL_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_LOCAL_VARIABLE",
        DefaultLanguageHighlighterColors.LOCAL_VARIABLE,
    )
    /** 普通属性高亮属性。 */
    val PROPERTY: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_PROPERTY", LOCAL_VARIABLE)
    /** 包级变量高亮属性。 */
    val PACKAGE_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_PACKAGE_VARIABLE",
        DefaultLanguageHighlighterColors.STATIC_FIELD,
    )
    /** 函数参数高亮属性。 */
    val PARAMETER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER)
    /** 实例属性高亮属性。 */
    val INSTANCE_PROPERTY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INSTANCE_PROPERTY",
        DefaultLanguageHighlighterColors.INSTANCE_FIELD,
    )
    /** 实例变量高亮属性。 */
    val INSTANCE_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INSTANCE_VARIABLE",
        DefaultLanguageHighlighterColors.INSTANCE_FIELD,
    )
    /** 被包装到引用对象中的变量高亮属性。 */
    val WRAPPED_INTO_REF: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_WRAPPED_INTO_REF",
        DefaultLanguageHighlighterColors.CLASS_NAME,
    )
    /** backing field 变量高亮属性。 */
    val BACKING_FIELD_VARIABLE: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BACKING_FIELD_VARIABLE")
    /** 扩展属性高亮属性。 */
    val EXTENSION_PROPERTY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_EXTENSION_PROPERTY",
        DefaultLanguageHighlighterColors.STATIC_FIELD,
    )

    /** 函数声明名称高亮属性。 */
    val FUNCTION_DECLARATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_FUNCTION_DECLARATION",
        DefaultLanguageHighlighterColors.FUNCTION_DECLARATION,
    )
    /** 函数调用名称高亮属性。 */
    val FUNCTION_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_FUNCTION_CALL",
        DefaultLanguageHighlighterColors.FUNCTION_CALL,
    )
    /** 包级函数调用名称高亮属性。 */
    val PACKAGE_FUNCTION_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_PACKAGE_FUNCTION_CALL",
        DefaultLanguageHighlighterColors.STATIC_METHOD,
    )
    /** 扩展函数调用名称高亮属性。 */
    val EXTENSION_FUNCTION_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_EXTENSION_FUNCTION_CALL",
        DefaultLanguageHighlighterColors.STATIC_METHOD,
    )
    /** 构造调用高亮属性。 */
    val CONSTRUCTOR_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_CONSTRUCTOR", DefaultLanguageHighlighterColors.FUNCTION_CALL)
    /** 变量作为函数调用时的高亮属性。 */
    val VARIABLE_AS_FUNCTION_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_VARIABLE_AS_FUNCTION")
    /** 类函数对象调用形态的高亮属性。 */
    val VARIABLE_AS_FUNCTION_LIKE_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_VARIABLE_AS_FUNCTION_LIKE")
    /** 函数字面量花括号和箭头高亮属性。 */
    val FUNCTION_LITERAL_BRACES_AND_ARROW: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_FUNCTION_LITERAL_BRACES_AND_ARROW")

    /** 注解名称高亮属性。 */
    val ANNOTATION: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ANNOTATION", DefaultLanguageHighlighterColors.METADATA)
    /** 注解参数名称高亮属性。 */
    val ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES",
        DefaultLanguageHighlighterColors.METADATA,
    )

    /** 宏声明名称高亮属性。 */
    val MACRO_DECLARATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_MACRO_DECLARATION")
    /** 宏调用名称高亮属性。 */
    val MACRO_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_MACRO_CALL")

    /** 智能转换后的值高亮属性。 */
    val SMART_CAST_VALUE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_SMART_CAST_VALUE")
    /** 智能常量高亮属性。 */
    val SMART_CONSTANT: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_SMART_CONSTANT")
    /** 智能转换 receiver 高亮属性。 */
    val SMART_CAST_RECEIVER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SMART_CAST_RECEIVER")
    /** 调试信息高亮属性。 */
    val DEBUG_INFO: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_DEBUG_INFO")

    /** 非法字符高亮属性。 */
    val BAD_CHARACTER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
    /** 标签高亮属性。 */
    val LABEL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_LABEL", DefaultLanguageHighlighterColors.LABEL)
    /** 解析到错误符号时的高亮属性。 */
    val RESOLVED_TO_ERROR: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_RESOLVED_TO_ERROR")
    /** 命名实参高亮属性。 */
    val NAMED_ARGUMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_NAMED_ARGUMENT")
    /** `<:` 符号高亮属性。 */
    val LT_COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_LT_COLON")
    /** 自定义属性声明中的实例属性高亮属性。 */
    val INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION",
        INSTANCE_PROPERTY,
    )
    /** 双冒号符号高亮属性。 */
    val DOUBLE_COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_DOUBLE_COLON")
    /** 非空断言符号高亮属性。 */
    val EXCLEXCL: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_EXCLEXCL")
}
