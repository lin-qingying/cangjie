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
import org.cangnova.cangjie.lexer.CjTokens

/**
 * `handle (commandPattern) { ... }` 子结构。
 *
 * `handle` 属于 `try` 的后缀分支，而不是独立表达式，
 * 所以这里建模为 TryExpression 的直接子节点，和 catch/finally 并列。
 */
class CjHandleClause(node: ASTNode) : CjElementImpl(node) {
    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitHandleClause(this, data)
    }

    /**
     * 保存 `handleKeyword`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val handleKeyword: PsiElement?
        get() = findChildByType(CjTokens.HANDLE_KEYWORD)

    /**
     * 保存 `commandPattern`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val commandPattern: CjCommandTypePattern?
        get() = findChildByClass(CjCommandTypePattern::class.java)

    /**
     * 保存 `handleBody`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val handleBody: CjBlockExpression?
        get() = findChildByClass(CjBlockExpression::class.java)
}
