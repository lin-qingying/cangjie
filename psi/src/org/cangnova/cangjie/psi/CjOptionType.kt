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

import org.cangnova.cangjie.lexer.CjTokens.QUEST
import org.cangnova.cangjie.psi.psiUtil.CjStubbedPsiUtil
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.CjTokenSets
import com.intellij.lang.ASTNode

/**
 * 表示 `CjOptionType`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjOptionType : CjElementImplStub<CangJiePlaceHolderStub<CjOptionType>>, CjTypeElement {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJiePlaceHolderStub<CjOptionType>) : super(stub, CjStubElementTypes.OPTIONAL_TYPE)

    /**
     * 提供 `getQuestionMarkNode` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getQuestionMarkNode(): ASTNode {
        return node.findChildByType(QUEST)!!
    }

    /**
     * 暴露 `typeArgumentsAsTypes`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeArgumentsAsTypes: List<CjTypeReference>
        get() {

            val innerType = getInnerType()
            return innerType?.typeArgumentsAsTypes ?: emptyList()
        }
    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitOptionType(this, data)
    }

    /**
     * 提供 `getInnerType` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    @IfNotParsed
    fun getInnerType(): CjTypeElement? {
        return CjStubbedPsiUtil.getStubOrPsiChild(
            this,
            CjTokenSets.TYPE_ELEMENT_TYPES,
            CjTypeElement.ARRAY_FACTORY,
        )
    }

    /**
     * 提供 `getModifierList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getModifierList(): CjModifierList? {
        return getStubOrPsiChild(CjStubElementTypes.MODIFIER_LIST)
    }

}
