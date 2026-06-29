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

import org.cangnova.cangjie.lexer.CjTokens.*
import org.cangnova.cangjie.psi.stubs.CangJieNameBasicReferenceExpressionStub
import org.cangnova.cangjie.psi.stubs.CangJieNameReferenceExpressionStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.name.*

/**
 * 定义 `CjCallableReference` 接口，约束仓颉 PSI节点或服务需要暴露的结构能力。
 */
interface CjCallableReference : CjReferenceExpression {
    /**
     * 保存 `callableReference`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val callableReference: CjSimpleNameExpression
    /**
     * 保存 `receiverExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val receiverExpression: CjExpression? get() = null
}

/**
 * 表示 `CjNameReferenceExpression`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjNameReferenceExpression :
    CjExpressionImplStub<CangJieNameReferenceExpressionStub>,
    CjSimpleNameExpression,
    CjCallableReference {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieNameReferenceExpressionStub) : super(stub, CjStubElementTypes.REFERENCE_EXPRESSION)

    /**
     * 暴露 `referencedName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedName: String
        get() {
            val stub = stub
            if (stub != null) {
                return stub.getReferencedName()
            }
            return CjSimpleNameExpressionImpl.getReferencedNameImpl(this)
        }

    /**
     * 暴露 `referencedNameAsName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedNameAsName: Name
        get() {
            return CjSimpleNameExpressionImpl.getReferencedNameAsNameImpl(this)
        }

    /**
     * 暴露 `referencedNameElement`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedNameElement: PsiElement
        get() {
            return findChildByType(NAME_REFERENCE_EXPRESSIONS) ?: this
        }
    /**
     * 保存 `typeArguments`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeArguments: List<CjTypeProjection>
        get() {

            return typeArgumentList?.arguments ?: emptyList()
        }
    /**
     * 保存 `typeArgumentList`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeArgumentList: CjTypeArgumentList?
        get() {

            return findChildByType<PsiElement>(CjNodeTypes.TYPE_ARGUMENT_LIST) as CjTypeArgumentList?
        }

    /**
     * 暴露 `identifier`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val identifier: PsiElement?
        get() {
            return findChildByType(IDENTIFIER)
        }

    /**
     * 暴露 `referencedNameElementType`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedNameElementType: IElementType
        get() {
            return CjSimpleNameExpressionImpl.getReferencedNameElementTypeImpl(this)
        }

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitSimpleNameExpression(this, data)
    }

    /**
     * 保存 `isPlaceholder`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isPlaceholder: Boolean
        get() = identifier?.text?.equals("_") == true

    companion object {
        private val NAME_REFERENCE_EXPRESSIONS = TokenSet.create(IDENTIFIER, THIS_KEYWORD, SUPER_KEYWORD, VARRAY_KEYWORD)
    }

    /**
     * 暴露 `callableReference`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val callableReference: CjNameReferenceExpression
        get() = this
}

/**
 * 表示 `CjNameBasicReferenceExpression`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjNameBasicReferenceExpression :
    CjExpressionImplStub<CangJieNameBasicReferenceExpressionStub>,
    CjSimpleNameExpression,
    CjCallableReference {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieNameBasicReferenceExpressionStub) : super(stub, CjStubElementTypes.BASIC_REFERENCE_EXPRESSION)

    /**
     * 暴露 `referencedName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedName: String
        get() {
            val stub = stub
            if (stub != null) {
                return stub.getReferencedName()
            }
            return CjSimpleNameExpressionImpl.getReferencedNameImpl(this)
        }

    /**
     * 暴露 `referencedNameAsName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedNameAsName: Name
        get() {
            return CjSimpleNameExpressionImpl.getReferencedNameAsNameImpl(this)
        }

    /**
     * 暴露 `referencedNameElement`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedNameElement: PsiElement
        get() {
            return findChildByType(NAME_REFERENCE_EXPRESSIONS) ?: this
        }
    /**
     * 保存 `typeArguments`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeArguments: List<CjTypeProjection>
        get() {

            return typeArgumentList?.arguments ?: emptyList()
        }
    /**
     * 保存 `typeArgumentList`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeArgumentList: CjTypeArgumentList?
        get() {

            return findChildByType<PsiElement>(CjNodeTypes.TYPE_ARGUMENT_LIST) as CjTypeArgumentList?
        }

    /**
     * 暴露 `identifier`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val identifier: PsiElement?
        get() {
            return findChildByType(IDENTIFIER)
        }

    /**
     * 暴露 `referencedNameElementType`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedNameElementType: IElementType
        get() {
            return CjSimpleNameExpressionImpl.getReferencedNameElementTypeImpl(this)
        }

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitSimpleNameExpression(this, data)
    }

    /**
     * 保存 `isPlaceholder`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isPlaceholder: Boolean
        get() = identifier?.text?.equals("_") == true

    companion object {
        private val NAME_REFERENCE_EXPRESSIONS = TokenSet.create(IDENTIFIER, THIS_KEYWORD, SUPER_KEYWORD, VARRAY_KEYWORD)
    }

    /**
     * 暴露 `callableReference`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val callableReference: CjNameBasicReferenceExpression
        get() = this
}
