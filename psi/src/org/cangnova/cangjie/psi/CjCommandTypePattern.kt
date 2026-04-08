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
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitCommandTypePattern(this, data)
    }

    val bindingNameElement: PsiElement?
        get() = findChildByType(CjTokens.IDENTIFIER)

    val bindingName: String?
        get() = bindingNameElement?.text

    val wildcardElement: PsiElement?
        get() = findChildByType(CjTokens.UNDERLINE)

    val isWildcard: Boolean
        get() = wildcardElement != null

    val typeReferences: List<CjTypeReference>
        get() = findChildrenByClass(CjTypeReference::class.java).toList()
}
