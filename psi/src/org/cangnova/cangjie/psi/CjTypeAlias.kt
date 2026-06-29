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

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.psiUtil.ClassIdCalculator
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.CangJieTypeAliasStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement

/**
 * 表示 `CjTypeAlias`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjTypeAlias : CjTypeParameterListOwnerStub<CangJieTypeAliasStub>, CjNamedDeclaration, CjClassLikeDeclaration {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieTypeAliasStub) : super(stub, CjStubElementTypes.TYPEALIAS)

    /**
     * 实现 `getClassId` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getClassId(): ClassId? {
        stub?.let { return it.getClassId() }
        return ClassIdCalculator.calculateClassId(this)
    }

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        return super.toString()
    }
    /**
     * 实现 `getPresentation` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)

    /**
     * 提供 `getTypeAliasKeyword` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    @IfNotParsed
    fun getTypeAliasKeyword(): PsiElement? =
        findChildByType(CjTokens.TYPE_KEYWORD)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitTypeAlias(this, data)
    }

    /**
     * 提供 `getTypeReference` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    @IfNotParsed
    fun getTypeReference(): CjTypeReference? {
        return if (stub != null) {
            val typeReferences =
                getStubOrPsiChildrenAsList<CjTypeReference, CangJiePlaceHolderStub<CjTypeReference>>(CjStubElementTypes.TYPE_REFERENCE)
            typeReferences[0]
        } else {
            findChildByType(CjNodeTypes.TYPE_REFERENCE)
        }
    }
}
