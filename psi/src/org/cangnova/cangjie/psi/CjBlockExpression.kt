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

import com.intellij.lang.Language
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.IncorrectOperationException
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.deleteSemicolon
import org.cangnova.cangjie.psi.psiUtil.getContainingCjFile
import org.cangnova.cangjie.psi.psiUtil.parentSubstitute
import java.util.*

/**
 * 表示 `CjBlockExpression`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
open class CjBlockExpression : LazyParseablePsiElement, CjElement, CjExpression, CjStatementExpression {
    constructor(type: IElementType, text: CharSequence?) : super(type, text)
    constructor(text: CharSequence?) : super(CjNodeTypes.BLOCK, text)

    /**
     * 实现 `getLanguage` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getLanguage(): Language {
        return CangJieLanguage
    }

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        return node.elementType.toString()
    }
    /**
     * 实现 `getContainingCjFile` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getContainingCjFile(): CjFile {
        return getContainingCjFile(this)

    }
    /**
     * 实现 `getContainingFile` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getContainingFile(): PsiFile {
        return super.getContainingFile()
    }

    /**
     * 实现 `getPsi` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <T : PsiElement> getPsi(clazz: Class<T>): T {
        return super.getPsi(clazz)
    }

    /**
     * 实现 `acceptChildren` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <D> acceptChildren(visitor: CjVisitor<Unit, D>, data: D) {
        CjPsiUtil.visitChildren(this, visitor, data)
    }

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitBlockExpression(this, data)
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
     * 实现 `delete` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    @Throws(IncorrectOperationException::class)
    override fun delete() {
        this.deleteSemicolon()
        super.delete()
    }

    /**
     * 实现 `getChildren` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getChildren(): Array<PsiElement> {
        var psiChild = firstChild

        var result: MutableList<PsiElement>? = null
        while (psiChild != null) {
            if (psiChild.node is CompositeElement) {
                if (result == null) result = ArrayList()
                result.add(psiChild)
            }
            psiChild = psiChild.nextSibling
        }
        return if (result == null) PsiElement.EMPTY_ARRAY else PsiUtilCore.toPsiElementArray(result)
    }

    /**
     * 实现 `getPsiOrParent` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getPsiOrParent(): CjElement {
        return this
    }

    /**
     * 实现 `getParent` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getParent(): PsiElement {
        val substitute = this.parentSubstitute
        return substitute ?: super.getParent()
    }



    /**
     * 保存 `firstStatement`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val firstStatement: CjExpression?
        get() = findChildByClass(CjExpression::class.java)

    /**
     * 保存 `lastStatement`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val lastStatement: CjExpression?
        get() {
            val statement = statements

            if (!statement.isEmpty()) {
                return statement[statement.size - 1]
            }

            return null
        }

    /**
     * 当前 block 的直接语句列表。
     *
     * block 可能包含嵌套 match、lambda 或局部 block；这些子结构内部的表达式不能泄漏为当前 block 的语句。
     */
    open val statements: List<CjExpression>
        get() {
            val result = ArrayList<CjExpression>()
            var child = firstChild
            while (child != null) {
                if (child is CjExpression) {
                    result.add(child)
                }
                child = child.nextSibling
            }
            return result
        }

    /**
     * 保存 `statementsWithoutReturnKeyword`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val statementsWithoutReturnKeyword: Set<CjExpression>
        /**
         * 没有return关键字的语句
         *
         * @return
         */
        get() {
            val returns =
                findChildrenByClass(CjReturnExpression::class.java)

            val result: MutableSet<CjExpression> = HashSet()

            for (statement in returns) {
                statement.returnedExpression?.let { result.add(it) }
            }

            val lastStatement = lastStatement

            if (lastStatement != null) {
                if (lastStatement !is CjReturnExpression && lastStatement !is CjDeclaration) {
                    result.add(lastStatement)
                }
            }
            return result
        }

    /**
     * 保存 `returnStatements`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val returnStatements: Set<CjExpression>
        get() {
            val returns: MutableSet<CjExpression> = HashSet(
                java.util.List.of(
                    *findChildrenByClass(
                        CjReturnExpression::class.java,
                    ),
                ),
            )

            val lastStatement = lastStatement
            if (lastStatement != null) {
                returns.add(lastStatement)
            }
            return returns
        }

    /**
     * 保存 `lastBracketRange`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val lastBracketRange: TextRange?
        get() {
            val rBrace = rBrace
            return rBrace?.textRange
        }

    /**
     * 保存 `rBrace`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val rBrace: PsiElement?
        get() = findPsiChildByType(CjTokens.RBRACE)

    /**
     * 保存 `lBrace`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val lBrace: PsiElement?
        get() = findPsiChildByType(CjTokens.LBRACE)
}
