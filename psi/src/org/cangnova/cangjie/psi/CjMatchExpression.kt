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
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

/**
 * 表示 `CjMatchExpression`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjMatchExpression(node: ASTNode) : CjExpressionImpl(node) {
    /**
     * 保存 `entries`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val entries
        get() = findChildrenByType<CjMatchEntry>(CjNodeTypes.MATCH_ENTRY)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitMatchExpression(this, data)
    }
    /**
     * 保存 `elseExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val elseExpression: CjExpression? get() {
        for (entry in entries) {
            if (entry.isElse) {
                return entry.expression
            }
        }
        return null
    }
    /**
     * 保存 `condition`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val condition: CjContainerNode?
        get() = findChildByType(CjNodeTypes.CONDITION)
    /**
     * 保存 `subjectExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val subjectExpression: CjExpression?
        get() = condition?.expression
    /**
     * 保存 `matchKeyword`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val matchKeyword: PsiElement
        get() = findChildByType(CjTokens.MATCH_KEYWORD)!!
    /**
     * 保存 `closeBrace`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val closeBrace: PsiElement?
        get() = findChildByType(CjTokens.RBRACE)
    /**
     * 保存 `openBrace`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val openBrace: PsiElement?
        get() = findChildByType(CjTokens.LBRACE)
    /**
     * 保存 `leftParenthesis`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val leftParenthesis: PsiElement?
        get() = findChildByType(CjTokens.LPAR)
    /**
     * 保存 `rightParenthesis`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val rightParenthesis: PsiElement?
        get() = findChildByType(CjTokens.RPAR)
}
