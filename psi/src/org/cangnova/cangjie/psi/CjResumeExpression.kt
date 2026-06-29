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
 * `resume` / `resume with <expr>` / `resume throwing <expr>` 的 PSI 壳节点。
 *
 * 这里不把 `with` 提升为全局关键字，PSI 仅按 `resume` 局部语法形状恢复。
 */
class CjResumeExpression(node: ASTNode) : CjExpressionImpl(node), CjStatementExpression {
    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitResumeExpression(this, data)
    }

    /**
     * 保存 `resumeKeyword`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val resumeKeyword: PsiElement?
        get() = findChildByType(CjTokens.RESUME_KEYWORD)

    /**
     * 保存 `throwingKeyword`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val throwingKeyword: PsiElement?
        get() = findChildByType(CjTokens.THROWING_KEYWORD)

    /**
     * 保存 `withExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    @get:IfNotParsed
    val withExpression: CjExpression?
        get() {
            if (throwingKeyword != null) return null
            return findChildByClass(CjExpression::class.java)
        }

    /**
     * 保存 `throwingExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    @get:IfNotParsed
    val throwingExpression: CjExpression?
        get() {
            if (throwingKeyword == null) return null
            return findChildByClass(CjExpression::class.java)
        }
}
