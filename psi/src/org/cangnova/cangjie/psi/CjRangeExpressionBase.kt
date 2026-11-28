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
 */

package org.cangnova.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.lexer.CjTokens

/**
 * 完整范围与下标切片共用的 AST PSI 结构。
 *
 * 范围表达式与切片表达式都由仓颉解析器表示为端点可缺省的范围节点；
 * 两者都不是普通一元或二元表达式，也不使用表达式 Stub。
 */
abstract class CjRangeExpressionBase(node: ASTNode) : CjExpressionImpl(node) {
    /** 由具体语法节点提供其范围运算符节点；完整范围用操作引用，切片用裸 token。 */
    protected abstract val rangeOperatorNode: ASTNode

    /** 范围起点；切片允许省略起点。 */
    val startExpression: CjExpression?
        get() = expressionAdjacentToOperator(isBefore = true)

    /** 范围终点；切片允许省略终点。 */
    val stopExpression: CjExpression?
        get() = expressionAdjacentToOperator(isBefore = false)

    /** 返回节点中实际的 `..` 或 `..=` token，不依赖范围是否有 operation-reference wrapper。 */
    val rangeOperatorToken: IElementType
        get() = rangeOperatorNode.firstChildNode?.elementType ?: rangeOperatorNode.elementType

    /** `..=` 包含终点；`..` 不包含终点。 */
    val isInclusive: Boolean
        get() = rangeOperatorToken == CjTokens.RANGEEQ

    /** 返回语法树中存在的范围端点。 */
    open fun getInnerExpressions(): List<CjExpression> = listOfNotNull(startExpression, stopExpression)

    private fun expressionAdjacentToOperator(isBefore: Boolean): CjExpression? {
        var sibling = if (isBefore) rangeOperatorNode.treePrev else rangeOperatorNode.treeNext
        while (sibling != null) {
            if (!isBefore && sibling.elementType == CjTokens.COLON) return null

            val expression = sibling.psi as? CjExpression
            if (expression != null) return expression
            sibling = if (isBefore) sibling.treePrev else sibling.treeNext
        }
        return null
    }
}
