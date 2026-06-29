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
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import org.cangnova.cangjie.name.*

/**
 * 表示 `CjFunctionLiteral`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjFunctionLiteral(node: ASTNode) : CjFunctionNotStubbed(node) {
    /**
     * 实现 `hasBlockBody` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBlockBody(): Boolean {
        return false
    }

    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String {
        return SpecialNames.ANONYMOUS_STRING
    }

    /**
     * 实现 `getNameIdentifier` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getNameIdentifier(): PsiElement? {
        return null
    }

    /**
     * 提供 `hasParameterSpecification` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun hasParameterSpecification(): Boolean {
        return findChildByType<PsiElement?>(CjTokens.ARROW) != null
    }

    /**
     * 暴露 `bodyExpression`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val bodyExpression: CjBlockExpression?
        get() {
            return super.bodyExpression as CjBlockExpression?
        }

    /**
     * 暴露 `equalsToken`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val equalsToken: PsiElement? = null
    /**
     * 保存 `lBrace`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val lBrace: PsiElement
        get() = findChildByType(CjTokens.LBRACE)!!

    /**
     * 保存 `rBrace`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    @get:IfNotParsed
    val rBrace: PsiElement?
        get() = findChildByType(CjTokens.RBRACE)

    /**
     * 保存 `arrow`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val arrow: PsiElement?
        get() = findChildByType(CjTokens.ARROW)

    /**
     * 暴露 `fqName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val fqName: FqName?
        get() = null

    /**
     * 实现 `hasBody` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBody(): Boolean {
        return bodyExpression != null
    }

    /**
     * 实现 `getUseScope` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getUseScope(): SearchScope {
        return LocalSearchScope(this)
    }
}
