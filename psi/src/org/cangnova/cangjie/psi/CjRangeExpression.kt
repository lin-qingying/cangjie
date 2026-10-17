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
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 表示 `CjRangeExpression`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjRangeExpression(node: ASTNode) : CjRangeExpressionBase(node), CjOperationExpression, CjReferenceExpression {
    /** 为现有 PSI 调用方保留端点别名；新代码应使用 [startExpression] 和 [stopExpression]。 */
    @Deprecated("Use startExpression for a range endpoint")
    val left: CjExpression?
        get() = startExpression

    /** 为现有 PSI 调用方保留端点别名；新代码应使用 [startExpression] 和 [stopExpression]。 */
    @Deprecated("Use stopExpression for a range endpoint")
    val right: CjExpression?
        get() = stopExpression

    override val operationReference: CjOperationReferenceExpression
        get() = findChildByType(CjStubElementTypes.OPERATION_REFERENCE)
            ?: error("No operation reference for range expression")

    val operationToken: IElementType
        get() = rangeOperatorToken

    override val rangeOperatorNode: ASTNode
        get() = operationReference.node

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitRangeExpression(this, data)
    }

    /**
     * 提供 `getInnerExpressions` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    override fun getInnerExpressions(): List<CjExpression> {
        return listOfNotNull(startExpression, stopExpression, step)
    }

    /**
     * 保存 `step`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val step: CjExpression?
        get() {
            val colon = findChildByType<PsiElement>(CjTokens.COLON) ?: return null

            var node = colon.node.treeNext
            while (node != null) {
                val psi = node.psi
                if (psi is CjExpression) {
                    return psi
                }
                node = node.treeNext
            }

            return null
        }

}
