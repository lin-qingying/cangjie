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
import org.cangnova.cangjie.psi.psiUtil.getTrailingCommaByClosingElement
import org.cangnova.cangjie.psi.stubs.CangJieCollectionLiteralExpressionStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

/**
 * 表示 `CjCollectionLiteralExpression`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjCollectionLiteralExpression :
    CjElementImplStub<CangJieCollectionLiteralExpressionStub>,
    CjReferenceExpression {
    constructor(stub: CangJieCollectionLiteralExpressionStub) : super(
        stub,
        CjStubElementTypes.COLLECTION_LITERAL_EXPRESSION,
    )

    constructor(node: ASTNode) : super(node)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitCollectionLiteralExpression(this, data)
    }

    /**
     * 保存 `leftBracket`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val leftBracket: PsiElement?
        get() {
            val astNode = node.findChildByType(CjTokens.LBRACKET)
            return astNode?.psi
        }

    /**
     * 保存 `rightBracket`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val rightBracket: PsiElement?
        get() {
            val astNode = node.findChildByType(CjTokens.RBRACKET)
            return astNode?.psi
        }

    /**
     * 保存 `trailingComma`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val trailingComma: PsiElement?
        get() {
            val rightBracket = rightBracket
            return getTrailingCommaByClosingElement(rightBracket)
        }

    /**
     * 保存 `innerExpressions`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val innerExpressions: List<CjExpression>
        get() {
            val stub = stub
            if (stub != null) {
                return Arrays.asList(
                    *stub.getChildrenByType(
                        CjNodeTypes.CONSTANT_EXPRESSIONS_TYPES,
                        CjExpression.EMPTY_ARRAY,
                    ),
                )
            }
            return PsiTreeUtil.getChildrenOfTypeAsList(
                this,
                CjExpression::class.java,
            )
        }
}
