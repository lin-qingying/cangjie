/*
 * Copyright 2025 LinQingYing. and contributors.
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
package org.cangnova.cangjie.psi.stubs.elements

import com.intellij.psi.tree.TokenSet

/**
 * 提供 `CjTokenSets` 单例，集中承载PSI Stub的共享状态、工厂或工具行为。
 */
object CjTokenSets {
    /**
     * 保存 `SUPER_TYPE_LIST_ENTRIES`，供PSI Stub流程读取节点结构或语义信息。
     */
    val SUPER_TYPE_LIST_ENTRIES: TokenSet =
        TokenSet.create(CjStubElementTypes.SUPER_TYPE_CALL_ENTRY, CjStubElementTypes.SUPER_TYPE_ENTRY)

    /**
     * 所有声明类型
     */
    val DECLARATION_TYPES: TokenSet = TokenSet.create(
        CjStubElementTypes.CLASS,
        CjStubElementTypes.INTERFACE,
        CjStubElementTypes.STRUCT,
        CjStubElementTypes.ENUM,
        CjStubElementTypes.EXTEND,
        CjStubElementTypes.FUNCTION,
        CjStubElementTypes.MAIN_FUNC,
        CjStubElementTypes.MACRO,
        CjStubElementTypes.VARIABLE,
        CjStubElementTypes.PROPERTY,
        CjStubElementTypes.TYPEALIAS,

        CjStubElementTypes.SECONDARY_CONSTRUCTOR,
        CjStubElementTypes.FINALIZER,
    )

    /**
     * 文件级别的声明类型
     */
    val FILE_DECLARATION_TYPES: TokenSet = TokenSet.create(
        CjStubElementTypes.CLASS,
        CjStubElementTypes.INTERFACE,
        CjStubElementTypes.STRUCT,
        CjStubElementTypes.ENUM,
        CjStubElementTypes.EXTEND,
        CjStubElementTypes.FUNCTION,
        CjStubElementTypes.MAIN_FUNC,
        CjStubElementTypes.MACRO,
        CjStubElementTypes.VARIABLE,
        CjStubElementTypes.TYPEALIAS,
    )

    /**
     * 类/接口/结构体/枚举成员声明类型
     */
    val CLASS_MEMBER_DECLARATION_TYPES: TokenSet = TokenSet.create(
        CjStubElementTypes.PRIMARY_CONSTRUCTOR,
        CjStubElementTypes.FUNCTION,
        CjStubElementTypes.FIELD,
        CjStubElementTypes.PROPERTY,
        CjStubElementTypes.SECONDARY_CONSTRUCTOR,
        CjStubElementTypes.FINALIZER,
        CjStubElementTypes.CLASS,
        CjStubElementTypes.INTERFACE,
        CjStubElementTypes.STRUCT,
        CjStubElementTypes.ENUM,
        CjStubElementTypes.TYPEALIAS,

    )

    /**
     * 保存 `INSIDE_DIRECTIVE_EXPRESSIONS`，供PSI Stub流程读取节点结构或语义信息。
     */
    val INSIDE_DIRECTIVE_EXPRESSIONS: TokenSet = TokenSet.create( //            IMPORT_DIRECTIVE_ITEM,
        CjStubElementTypes.DOT_QUALIFIED_EXPRESSION,
        CjStubElementTypes.REFERENCE_EXPRESSION,
    )
    /**
     * 保存 `TYPE_ELEMENT_TYPES`，供PSI Stub流程读取节点结构或语义信息。
     */
    val TYPE_ELEMENT_TYPES: TokenSet = TokenSet.create(
        CjStubElementTypes.THIS_TYPE,
        CjStubElementTypes.VARRAY_TYPE,
        CjStubElementTypes.USER_TYPE,
        CjStubElementTypes.BASIC_TYPE,
        CjStubElementTypes.TUPLE_TYPE,
        CjStubElementTypes.FUNCTION_TYPE,
        CjStubElementTypes.OPTIONAL_TYPE,
        CjStubElementTypes.PARENTHESIZED_TYPE,
    )
}
