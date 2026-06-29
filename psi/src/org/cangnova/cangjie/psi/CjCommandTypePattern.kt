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
 * `nameOrWildcard : Type | Type | ...` 的 effect command pattern。
 *
 * 它只服务于 `handle (...)`，因此单独建模，避免混入 match/catch 的通用 pattern 语义。
 */
class CjCommandTypePattern(node: ASTNode) : CjElementImpl(node) {
    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitCommandTypePattern(this, data)
    }

    /**
     * 保存 `bindingNameElement`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val bindingNameElement: PsiElement?
        get() = findChildByType(CjTokens.IDENTIFIER)

    /**
     * 保存 `bindingName`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val bindingName: String?
        get() = bindingNameElement?.text

    /**
     * 保存 `wildcardElement`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val wildcardElement: PsiElement?
        get() = findChildByType(CjTokens.UNDERLINE)

    /**
     * 保存 `isWildcard`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isWildcard: Boolean
        get() = wildcardElement != null

    /**
     * 保存 `typeReferences`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeReferences: List<CjTypeReference>
        get() = findChildrenByClass(CjTypeReference::class.java).toList()
}
