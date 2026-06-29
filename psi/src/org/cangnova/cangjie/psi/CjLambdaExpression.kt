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

import org.cangnova.cangjie.psi.CjNodeTypes.FUNCTION_LITERAL
import org.cangnova.cangjie.psi.CjNodeTypes.LAMBDA_EXPRESSION
import org.cangnova.cangjie.lexer.CjTokens.LBRACE
import org.cangnova.cangjie.lexer.CjTokens.RBRACE
import org.cangnova.cangjie.psi.psiUtil.getContainingCjFile
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement

/**
 * 表示 `CjLambdaExpression`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjLambdaExpression(text: CharSequence?) :
    LazyParseablePsiElement(LAMBDA_EXPRESSION, text),
    CjExpression {

    /**
     * 保存 `functionLiteral`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val functionLiteral: CjFunctionLiteral
        get() = findChildByType(FUNCTION_LITERAL)?.getPsi(CjFunctionLiteral::class.java)!!

    /**
     * 保存 `valueParameters`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val valueParameters: List<CjParameter>
        get() = functionLiteral.valueParameters

    /**
     * 保存 `parameterList`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val parameterList: CjParameterList?
        get() = functionLiteral.valueParameterList
    /**
     * 保存 `bodyExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val bodyExpression: CjBlockExpression?
        get() = functionLiteral.bodyExpression

    /**
     * 提供 `hasDeclaredReturnType` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun hasDeclaredReturnType(): Boolean {
        return functionLiteral.typeReference != null
    }

    /**
     * 提供 `asElement` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun asElement(): CjElement {
        return this
    }

    /**
     * 保存 `leftCurlyBrace`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val leftCurlyBrace: ASTNode
        get() = functionLiteral.node.findChildByType(LBRACE)!!

    /**
     * 保存 `rightCurlyBrace`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val rightCurlyBrace: ASTNode?
        get() = functionLiteral.node.findChildByType(RBRACE)

    /**
     * 实现 `acceptChildren` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <D> acceptChildren(visitor: CjVisitor<Unit, D>, data: D) {
        CjPsiUtil.visitChildren<D>(this, visitor, data)
    }

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitLambdaExpression(this, data)
    }

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is CjVisitor<*, *>) {
            @Suppress("UNCHECKED_CAST")
            accept(visitor as CjVisitor<Any?, Any?>, null as Any?)
        } else {
            visitor.visitElement(this)
        }
    }

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        return node.elementType.toString()
    }

    /**
     * 实现 `getPsiOrParent` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getPsiOrParent(): CjElement {
        return this
    }

    /**
     * 实现 `getContainingCjFile` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getContainingCjFile(): CjFile {
        return getContainingCjFile(this)
    }

    /**
     * 提供 `shouldChangeModificationCount` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    @Suppress("unused") // keep for compatibility with potential plugins
    fun shouldChangeModificationCount(place: PsiElement?): Boolean {
        return false
    }
}
