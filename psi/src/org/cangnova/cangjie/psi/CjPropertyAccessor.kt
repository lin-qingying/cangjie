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
import org.cangnova.cangjie.psi.stubs.CangJiePropertyAccessorStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * 表示 `CjPropertyAccessor`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjPropertyAccessor :
    CjDeclarationStub<CangJiePropertyAccessorStub>,
    CjDeclarationWithBody,
    CjModifierListOwner,
    CjDeclarationWithInitializer {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePropertyAccessorStub) : super(stub, CjStubElementTypes.PROPERTY_ACCESSOR)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPropertyAccessor(this, data)
    }

    /**
     * 保存 `isSetter`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isSetter: Boolean
        get() {
            val stub: CangJiePropertyAccessorStub? = stub
            if (stub != null) {
                return !stub.isGetter()
            }
            return findChildByType<PsiElement?>(CjTokens.SET_KEYWORD) != null
        }

    /**
     * 保存 `isGetter`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isGetter: Boolean
        get() {
            val stub: CangJiePropertyAccessorStub? = stub
            if (stub != null) {
                return stub.isGetter()
            }
            return findChildByType<PsiElement?>(CjTokens.GET_KEYWORD) != null
        }

    /**
     * 保存 `parameterList`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val parameterList: CjParameterList?
        get() = getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST)

    /**
     * 保存 `parameter`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val parameter: CjParameter?
        get() {
            val parameterList: CjParameterList = parameterList ?: return null
            val parameters: List<CjParameter> = parameterList.parameters
            if (parameters.isEmpty()) return null
            return parameters[0]
        }

    /**
     * 暴露 `valueParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameters: List<CjParameter>
        get() {
            val parameter: CjParameter = parameter ?: return emptyList()
            return listOf(parameter)
        }

    /**
     * 暴露 `bodyExpression`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val bodyExpression: CjExpression?
        get() {
            val stub: CangJiePropertyAccessorStub? = stub
            if (stub != null) {
                if (!stub.hasBody()) {
                    return null
                }

                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            return findChildByClass(CjExpression::class.java)
        }

    /**
     * 暴露 `bodyBlockExpression`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val bodyBlockExpression: CjBlockExpression?
        get() {
            val stub: CangJiePropertyAccessorStub? = stub
            if (stub != null) {
                if (!(stub.hasBlockBody() && stub.hasBody())) {
                    return null
                }
                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            val bodyExpression: CjExpression? = findChildByClass(
                CjExpression::class.java,
            )
            if (bodyExpression is CjBlockExpression) {
                return bodyExpression
            }

            return null
        }

    /**
     * 实现 `hasBlockBody` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBlockBody(): Boolean {
        val stub: CangJiePropertyAccessorStub? = stub
        if (stub != null) {
            return stub.hasBlockBody()
        }
        return equalsToken == null
    }

    /**
     * 实现 `hasBody` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBody(): Boolean {
        val stub: CangJiePropertyAccessorStub? = stub
        if (stub != null) {
            return stub.hasBody()
        }
        return bodyExpression != null
    }

    /**
     * 暴露 `equalsToken`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val equalsToken: PsiElement?
        get() = findChildByType(CjTokens.EQ)

    /**
     * 实现 `hasDeclaredReturnType` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasDeclaredReturnType(): Boolean {
        return true
    }

    /**
     * 保存 `returnTypeReference`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val returnTypeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    /**
     * 保存 `namePlaceholder`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val namePlaceholder: PsiElement
        get() {
            val get: PsiElement? = findChildByType(CjTokens.GET_KEYWORD)
            if (get != null) {
                return get
            }
            return findChildByType(CjTokens.SET_KEYWORD)!!
        }

    /**
     * 保存 `rightParenthesis`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val rightParenthesis: PsiElement?
        get() = findChildByType(CjTokens.RPAR)

    /**
     * 保存 `leftParenthesis`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val leftParenthesis: PsiElement?
        get() = findChildByType(CjTokens.LPAR)

    /**
     * 暴露 `initializer`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val initializer: CjExpression?
        get() = PsiTreeUtil.getNextSiblingOfType(
            equalsToken,
            CjExpression::class.java,
        )

    /**
     * 实现 `hasInitializer` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasInitializer(): Boolean {
        return initializer != null
    }

    /**
     * 保存 `property`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val property: CjProperty
        get() {
            return parent!!.parent as CjProperty
        }

    /**
     * 实现 `getTextOffset` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getTextOffset(): Int {
        return namePlaceholder.textRange.startOffset
    }
}
