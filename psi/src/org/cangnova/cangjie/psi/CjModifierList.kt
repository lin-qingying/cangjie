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

import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.psi.stubs.CangJieModifierListStub
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.TokenSet

/**
 * 表示 `CjModifierList`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
abstract class CjModifierList : CjElementImplStub<CangJieModifierListStub>{
    constructor(stub: CangJieModifierListStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    constructor(node: ASTNode) : super(node)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitModifierList(this, data)
    }

    /**
     * 提供 `hasModifier` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun hasModifier(tokenType: CjKeywordToken): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasModifier(tokenType)
        }
        return getModifier(tokenType) != null
    }

    /**
     * 提供 `getModifier` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getModifier(tokenType: CjKeywordToken): PsiElement? {
        return findChildByType(tokenType)
    }

    /**
     * 提供 `getModifier` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getModifier(tokenTypes: TokenSet): PsiElement? {
        return findChildByType(tokenTypes)
    }

    /**
     * 保存 `owner`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val owner: PsiElement
        get() = parentByStub

    /**
     * 实现 `deleteChildInternal` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun deleteChildInternal(child: ASTNode) {
        super.deleteChildInternal(child)
        if (firstChild == null) {
            delete()
        }
    }
}
