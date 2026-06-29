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

import com.google.common.collect.Lists
import com.intellij.lang.ASTNode

/**
 * 表示 `CjCallExpression`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
open class CjCallExpression(node: ASTNode) : CjExpressionImpl(node), CjCallElement, CjReferenceExpression {
    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitCallExpression(this, data)
    }

    /**
     * 暴露 `lambdaArguments`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val lambdaArguments: List<CjLambdaArgument>
        get() {
            return findChildrenByType<CjLambdaArgument>(CjNodeTypes.LAMBDA_ARGUMENT)
        }
    /**
     * 保存 `referenceExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val referenceExpression: CjSimpleNameExpression? get() = calleeExpression as? CjSimpleNameExpression

    /**
     * 暴露 `typeArguments`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeArguments: List<CjTypeProjection>
        get() {
            return typeArgumentList?.arguments ?: emptyList()
        }

    /**
     * 暴露 `typeArgumentList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeArgumentList: CjTypeArgumentList?
        get() {
            val directTypeArgumentList = findChildByType<CjTypeArgumentList>(CjNodeTypes.TYPE_ARGUMENT_LIST)
            return directTypeArgumentList
                ?: findTypeArgumentListInCalleeSyntax()
                ?: referenceExpression?.getTypeArgumentList()
        }

    /**
     * 执行 `findTypeArgumentListInCalleeSyntax` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun findTypeArgumentListInCalleeSyntax(): CjTypeArgumentList? {
        fun ASTNode.findTypeArgumentList(): CjTypeArgumentList? {
            if (elementType == CjNodeTypes.TYPE_ARGUMENT_LIST) {
                return psi as? CjTypeArgumentList
            }
            var child = firstChildNode
            while (child != null) {
                child.findTypeArgumentList()?.let { return it }
                child = child.treeNext
            }
            return null
        }

        var child = node.firstChildNode
        while (child != null) {
            if (child.elementType == CjNodeTypes.VALUE_ARGUMENT_LIST || child.elementType == CjNodeTypes.LAMBDA_ARGUMENT) {
                return null
            }
            child.findTypeArgumentList()?.let { return it }
            child = child.treeNext
        }
        return null
    }

    /**
     * 暴露 `calleeExpression`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val calleeExpression: CjExpression?
        get() {
            return findChildByClass(CjExpression::class.java)
        }

    /**
     * 暴露 `valueArgumentList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueArgumentList: CjValueArgumentList?
        get() {

            return findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST) as CjValueArgumentList?
        }

    /**
     * 暴露 `valueArguments`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueArguments: List<CjValueArgument>
        get() {

            val valueArgumentsInParentheses =
                valueArgumentList?.arguments ?: emptyList<CjValueArgument>()
            val functionLiteralArguments: List<CjLambdaArgument> =
                lambdaArguments
            if (functionLiteralArguments.isEmpty()) {
                return valueArgumentsInParentheses
            }
            val allValueArguments: MutableList<CjValueArgument> =
                Lists.newArrayList<CjValueArgument>()
            allValueArguments.addAll(valueArgumentsInParentheses)
            allValueArguments.addAll(functionLiteralArguments)
            return allValueArguments
        }

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        return node.elementType.toString()
    }
}
