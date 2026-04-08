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
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitResumeExpression(this, data)
    }

    val resumeKeyword: PsiElement?
        get() = findChildByType(CjTokens.RESUME_KEYWORD)

    val throwingKeyword: PsiElement?
        get() = findChildByType(CjTokens.THROWING_KEYWORD)

    @get:IfNotParsed
    val withExpression: CjExpression?
        get() {
            if (throwingKeyword != null) return null
            return findChildByClass(CjExpression::class.java)
        }

    @get:IfNotParsed
    val throwingExpression: CjExpression?
        get() {
            if (throwingKeyword == null) return null
            return findChildByClass(CjExpression::class.java)
        }
}
