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
import org.cangnova.cangjie.psi.stubs.CangJieCatchParameterStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

/**
 * 表示 `CjCatchParameter`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjCatchParameter : CjNamedDeclarationStub<CangJieCatchParameterStub>, CjParameterBase {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieCatchParameterStub) : super(stub, CjStubElementTypes.CATCH_PARAMETER)

    /**
     * 保存 `typeReferences`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeReferences: List<CjTypeReference>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)

    /**
     * 实现 `hasLetOrVar` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasLetOrVar(): Boolean {
        return false
    }

    /**
     * 实现 `hasDefaultValue` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasDefaultValue(): Boolean {
        return false
    }

    /**
     * 暴露 `valueParameterList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameterList: CjParameterList? = null
    /**
     * 暴露 `valueParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameters: List<CjParameter> = emptyList()
    /**
     * 暴露 `typeReference`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeReference: CjTypeReference? get() = null

    /**
     * 实现 `setTypeReference` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return typeReference
    }

    /**
     * 暴露 `colon`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)
    /**
     * 暴露 `typeParameterList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeParameterList: CjTypeParameterList? = null
    /**
     * 暴露 `typeConstraintList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeConstraintList: CjTypeConstraintList? = null
    /**
     * 暴露 `typeConstraints`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeConstraints: List<CjTypeConstraint> = emptyList()
    /**
     * 暴露 `typeParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeParameters: List<CjTypeParameter> = emptyList()
    /**
     * 暴露 `letOrVarKeyword`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val letOrVarKeyword: PsiElement? = null
    /**
     * 保存 `equalsToken`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val equalsToken: PsiElement?
        get() = findChildByType(CjTokens.EQ)
}
