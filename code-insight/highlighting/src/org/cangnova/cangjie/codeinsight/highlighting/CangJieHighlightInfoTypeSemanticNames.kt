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

import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.openapi.editor.colors.TextAttributesKey

/**
 * 仓颉结构/语义高亮使用的 HighlightInfoType 目录。
 *
 * TextAttributesKey 描述“显示成什么属性”，HighlightInfoType 描述 IntelliJ 高亮管线中的信息类型。
 * code-insight 在这里统一生成二者的对应关系，避免各使用方重复写 key -> HighlightInfoType 映射。
 */
object CangJieHighlightInfoTypeSemanticNames {
    /** 自定义属性声明中的实例属性高亮信息类型。 */
    val INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION: HighlightInfoType =
        createSymbolTypeInfo(CangJieHighlightingColors.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION)

    /** 通用关键字高亮信息类型。 */
    val KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.KEYWORD)
    /** `let` 关键字高亮信息类型。 */
    val LET_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.LET_KEYWORD)
    /** `mut` 关键字高亮信息类型。 */
    val MUT_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.MUT_KEYWORD)
    /** `prop` 关键字高亮信息类型。 */
    val PROP_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PROP_KEYWORD)
    /** `var` 关键字高亮信息类型。 */
    val VAR_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.VAR_KEYWORD)
    /** `const` 关键字高亮信息类型。 */
    val CONST_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.CONST_KEYWORD)
    /** `quote` 关键字高亮信息类型。 */
    val QUOTE_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.QUOTE_KEYWORD)
    /** 静态声明关键字高亮信息类型。 */
    val STATIC_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.STATIC_KEYWORD)

    /** 行注释高亮信息类型。 */
    val LINE_COMMENT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.LINE_COMMENT)
    /** 块注释高亮信息类型。 */
    val BLOCK_COMMENT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.BLOCK_COMMENT)
    /** 文档注释高亮信息类型。 */
    val DOC_COMMENT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.DOC_COMMENT)
    /** CDoc 标签高亮信息类型。 */
    val CDOC_TAG: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.CDOC_TAG)
    /** CDoc 链接高亮信息类型。 */
    val CDOC_LINK: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.CDOC_LINK)

    /** 数字字面量高亮信息类型。 */
    val NUMBER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.NUMBER)
    /** 字符串字面量高亮信息类型。 */
    val STRING: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.STRING)
    /** 合法字符串转义高亮信息类型。 */
    val STRING_ESCAPE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.STRING_ESCAPE)
    /** 非法字符串转义高亮信息类型。 */
    val INVALID_STRING_ESCAPE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.INVALID_STRING_ESCAPE)

    /** 运算符符号高亮信息类型。 */
    val OPERATOR_SIGN: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.OPERATOR_SIGN)
    /** 圆括号高亮信息类型。 */
    val PARENTHESIS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PARENTHESIS)
    /** 花括号高亮信息类型。 */
    val BRACES: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.BRACES)
    /** 方括号高亮信息类型。 */
    val BRACKETS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.BRACKETS)
    /** 逗号高亮信息类型。 */
    val COMMA: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.COMMA)
    /** 分号高亮信息类型。 */
    val SEMICOLON: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SEMICOLON)
    /** 冒号高亮信息类型。 */
    val COLON: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.COLON)
    /** 点号高亮信息类型。 */
    val DOT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.DOT)
    /** 安全访问运算符高亮信息类型。 */
    val SAFE_ACCESS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SAFE_ACCESS)
    /** 问号符号高亮信息类型。 */
    val QUEST: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.QUEST)
    /** 箭头符号高亮信息类型。 */
    val ARROW: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ARROW)

    /** 类型定义名称高亮信息类型。 */
    val TYPE_DEFINED: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.TYPE_DEFINED)
    /** 类型引用名称高亮信息类型。 */
    val TYPE_REFERENCE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.TYPE_REFERENCE)
    /** class 名称高亮信息类型。 */
    val CLASS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.CLASS)
    /** 类型参数名称高亮信息类型。 */
    val TYPE_PARAMETER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.TYPE_PARAMETER)
    /** 抽象 class 名称高亮信息类型。 */
    val ABSTRACT_CLASS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ABSTRACT_CLASS)
    /** interface 名称高亮信息类型。 */
    val INTERFACE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.INTERFACE)
    /** struct 名称高亮信息类型。 */
    val STRUCT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.STRUCT)
    /** enum 名称高亮信息类型。 */
    val ENUM: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ENUM)
    /** enum 构造项高亮信息类型。 */
    val ENUM_CONSTRUCTOR: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ENUM_CONSTRUCTOR)
    /** typealias 名称高亮信息类型。 */
    val TYPE_ALIAS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.TYPE_ALIAS)

    /** 可变变量高亮信息类型。 */
    val MUTABLE_VARIABLE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.MUTABLE_VARIABLE)
    /** 可变属性高亮信息类型。 */
    val MUTABLE_PROPERTY: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.MUTABLE_PROPERTY)
    /** 局部变量高亮信息类型。 */
    val LOCAL_VARIABLE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.LOCAL_VARIABLE)
    /** 普通属性高亮信息类型。 */
    val PROPERTY: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PROPERTY)
    /** 包级变量高亮信息类型。 */
    val PACKAGE_VARIABLE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PACKAGE_VARIABLE)
    /** 参数高亮信息类型。 */
    val PARAMETER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PARAMETER)
    /** 实例属性高亮信息类型。 */
    val INSTANCE_PROPERTY: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.INSTANCE_PROPERTY)
    /** 实例变量高亮信息类型。 */
    val INSTANCE_VARIABLE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.INSTANCE_VARIABLE)
    /** 被包装为引用对象的变量高亮信息类型。 */
    val WRAPPED_INTO_REF: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.WRAPPED_INTO_REF)
    /** backing field 变量高亮信息类型。 */
    val BACKING_FIELD_VARIABLE: HighlightInfoType =
        createSymbolTypeInfo(CangJieHighlightingColors.BACKING_FIELD_VARIABLE)
    /** 扩展属性高亮信息类型。 */
    val EXTENSION_PROPERTY: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.EXTENSION_PROPERTY)

    /** 函数声明高亮信息类型。 */
    val FUNCTION_DECLARATION: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.FUNCTION_DECLARATION)
    /** 函数调用高亮信息类型。 */
    val FUNCTION_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.FUNCTION_CALL)
    /** 包级函数调用高亮信息类型。 */
    val PACKAGE_FUNCTION_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PACKAGE_FUNCTION_CALL)
    /** 扩展函数调用高亮信息类型。 */
    val EXTENSION_FUNCTION_CALL: HighlightInfoType =
        createSymbolTypeInfo(CangJieHighlightingColors.EXTENSION_FUNCTION_CALL)
    /** 构造调用高亮信息类型。 */
    val CONSTRUCTOR_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.CONSTRUCTOR_CALL)
    /** 变量作为函数调用高亮信息类型。 */
    val VARIABLE_AS_FUNCTION_CALL: HighlightInfoType =
        createSymbolTypeInfo(CangJieHighlightingColors.VARIABLE_AS_FUNCTION_CALL)
    /** 类函数对象调用形态高亮信息类型。 */
    val VARIABLE_AS_FUNCTION_LIKE_CALL: HighlightInfoType =
        createSymbolTypeInfo(CangJieHighlightingColors.VARIABLE_AS_FUNCTION_LIKE_CALL)
    /** 函数字面量花括号与箭头高亮信息类型。 */
    val FUNCTION_LITERAL_BRACES_AND_ARROW: HighlightInfoType =
        createSymbolTypeInfo(CangJieHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW)

    /** 注解名称高亮信息类型。 */
    val ANNOTATION: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ANNOTATION)
    /** 注解参数名称高亮信息类型。 */
    val ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES: HighlightInfoType =
        createSymbolTypeInfo(CangJieHighlightingColors.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES)

    /** 宏声明名称高亮信息类型。 */
    val MACRO_DECLARATION: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.MACRO_DECLARATION)
    /** 宏调用名称高亮信息类型。 */
    val MACRO_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.MACRO_CALL)

    /** 智能转换后的值高亮信息类型。 */
    val SMART_CAST_VALUE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SMART_CAST_VALUE)
    /** 智能常量高亮信息类型。 */
    val SMART_CONSTANT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SMART_CONSTANT)
    /** 智能转换 receiver 高亮信息类型。 */
    val SMART_CAST_RECEIVER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SMART_CAST_RECEIVER)
    /** 调试信息高亮信息类型。 */
    val DEBUG_INFO: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.DEBUG_INFO)

    /** 非法字符高亮信息类型。 */
    val BAD_CHARACTER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.BAD_CHARACTER)
    /** 标签高亮信息类型。 */
    val LABEL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.LABEL)
    /** 解析到错误符号时的高亮信息类型。 */
    val RESOLVED_TO_ERROR: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.RESOLVED_TO_ERROR)
    /** 命名实参高亮信息类型。 */
    val NAMED_ARGUMENT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.NAMED_ARGUMENT)
    /** `<:` 符号高亮信息类型。 */
    val LT_COLON: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.LT_COLON)

    /**
     * 用指定文本属性 key 创建语义符号高亮信息类型。
     */
    private fun createSymbolTypeInfo(attributesKey: TextAttributesKey): HighlightInfoType =
        HighlightInfoType.HighlightInfoTypeImpl(HighlightInfoType.SYMBOL_TYPE_SEVERITY, attributesKey, false)
}
