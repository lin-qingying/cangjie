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
 * 表示 `CjTryExpression`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjTryExpression(node: ASTNode) : CjExpressionImpl(node) {
    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitTryExpression(this, data)
    }

    /**
     * 保存 `tryResourceList`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val tryResourceList: CjTryResourceList? get() = findChildByClass(CjTryResourceList::class.java)
    /**
     * 保存 `catchBody`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val catchBody: CjExpression?
        get() = findChildByClass(CjExpression::class.java)
    /**
     * 保存 `tryBlock`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val tryBlock: CjBlockExpression
        get() = findChildByType<PsiElement>(CjNodeTypes.BLOCK) as CjBlockExpression
    /**
     * 保存 `catchClauses`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val catchClauses: List<CjCatchClause>
        get() = findChildrenByType(CjNodeTypes.CATCH)
    /**
     * `handle` 在语法上属于 `try` 的后缀子结构，而不是独立表达式。
     * 直接暴露列表，便于 raw-CFIR/CFIR 沿官方 AST 形状继续构建。
     */
    val handleClauses: List<CjHandleClause>
        get() = findChildrenByType(CjNodeTypes.HANDLE)
    /**
     * 保存 `finallyBlock`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val finallyBlock: CjFinallySection?
        get() = findChildByType<PsiElement>(CjNodeTypes.FINALLY) as CjFinallySection?
    /**
     * 保存 `tryKeyword`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val tryKeyword: PsiElement?
        get() = findChildByType(CjTokens.TRY_KEYWORD)
}
