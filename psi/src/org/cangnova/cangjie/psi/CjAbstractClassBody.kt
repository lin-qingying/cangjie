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

package org.cangnova.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.stubs.CangJieEnumStub
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.*
import org.cangnova.cangjie.psi.stubs.elements.CjTokenSets

/**
 * 表示 `CjInterfaceBody`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjInterfaceBody : CjAbstractClassBody {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjInterfaceBody>) : super(stub, INTERFACE_BODY)
}

/**
 * 表示 `CjEnumBody`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjEnumBody : CjAbstractClassBody {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjEnumBody>) : super(stub, ENUM_BODY)

    /**
     * 保存 `constructor`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val constructor get() = getStubOrPsiChildrenAsList(ENUM_CONSTRUCTOR)
    /**
     * 是否非穷枚举
     */
    val isNonExhaustive: Boolean
        get() {

            return findChildByType<PsiElement>(CjTokens.ELLIPSIS) != null
        }
}

/**
 * 表示 `CjClassBody`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjClassBody : CjAbstractClassBody {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjEnumBody>) : super(stub, CLASS_BODY)
}

/**
 * 表示 `CjAbstractClassBody`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
abstract class CjAbstractClassBody :
    CjElementImplStub<CangJiePlaceHolderStub<out CjAbstractClassBody>>,
    CjDeclarationContainer {
    /**
     * 保存 `lBraceTokenSet` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val lBraceTokenSet = TokenSet.create(CjTokens.LBRACE)
    /**
     * 保存 `rBraceTokenSet` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val rBraceTokenSet = TokenSet.create(CjTokens.RBRACE)

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<out CjAbstractClassBody>, nodeType: IStubElementType<*, *>) : super(
        stub,
        nodeType,
    )

    constructor(stub: CangJiePlaceHolderStub<CjAbstractClassBody>) : super(stub, CLASS_BODY)

    /**
     * 实现 `getParent` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getParent() = parentByStub
    /**
     * 保存 `secondaryConstructors`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    internal val secondaryConstructors: List<CjSecondaryConstructor>
        get() = getStubOrPsiChildrenAsList(SECONDARY_CONSTRUCTOR)
    /**
     * 保存 `primaryConstructors`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    internal val primaryConstructors: List<CjPrimaryConstructor>
        get() = getStubOrPsiChildrenAsList(PRIMARY_CONSTRUCTOR)
    /**
     * 保存 `finalizers`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    internal val finalizers: List<CjFinalizer>
        get() = getStubOrPsiChildrenAsList(FINALIZER)

    /**
     * @return annotations that do not belong to any declaration due to incomplete code or syntax errors
     */
//    val danglingAnnotations: List<CjAnnotation>
//        get() = danglingModifierLists.flatMap { it.annotationEntries }

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        return node.elementType.toString()
    }

    /**
     * 保存 `properties`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val properties: List<CjProperty>
        get() = getStubOrPsiChildrenAsList(PROPERTY)
    /**
     * 保存 `variables`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val variables: List<CjFieldVariable>
        get() = getStubOrPsiChildrenAsList(FIELD)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? = visitor.visitClassBody(this, data)
    /**
     * 暴露 `declarations`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val declarations: List<CjDeclaration>
        get() = stub?.getChildrenByType(CjTokenSets.CLASS_MEMBER_DECLARATION_TYPES, CjDeclaration.ARRAY_FACTORY)?.toList()
            ?: PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java)

    /**
     * 保存 `rBrace`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val rBrace: PsiElement?
        get() = node.getChildren(rBraceTokenSet).singleOrNull()?.psi

    /**
     * 保存 `lBrace`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val lBrace: PsiElement?
        get() = node.getChildren(lBraceTokenSet).singleOrNull()?.psi
}
