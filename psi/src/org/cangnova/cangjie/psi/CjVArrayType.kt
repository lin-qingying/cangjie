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
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

/**
 * 表示 `CjVArrayType`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjVArrayType : CjElementImplStub<CangJiePlaceHolderStub<CjVArrayType>>, CjTypeElement {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJiePlaceHolderStub<CjVArrayType>) : super(stub, CjStubElementTypes.VARRAY_TYPE)

    // 整数字面量
    /**
     * 保存 `literal`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val literal: PsiElement? get() = findChildByType(CjTokens.INTEGER_LITERAL)

    //    类型参数
    /**
     * 保存 `typeProjection`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeProjection = typeArguments.firstOrNull()

    /**
     * 保存 `typeReference`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeReference = typeProjection?.typeReference
    /**
     * 保存 `typeElement`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeElement = typeReference?.typeElement
    /**
     * 保存 `typeArgumentList`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeArgumentList: CjTypeArgumentList?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_ARGUMENT_LIST)

    /**
     * 保存 `typeArguments`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeArguments: List<CjTypeProjection>
        get() {
            val typeArgumentList = typeArgumentList
            return typeArgumentList?.arguments ?: emptyList()
        }

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitVArrayType(this, data)
    }
}
