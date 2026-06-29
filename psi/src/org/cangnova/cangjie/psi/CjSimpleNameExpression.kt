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
import org.cangnova.cangjie.name.Name
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

/**
 * 定义 `CjSimpleNameExpression` 接口，约束仓颉 PSI节点或服务需要暴露的结构能力。
 */
interface CjSimpleNameExpression : CjReferenceExpression {

    /**
     * 保存 `referencedName`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val referencedName: String

    /**
     * 保存 `referencedNameAsName`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val referencedNameAsName: Name

    /**
     * 保存 `referencedNameElement`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val referencedNameElement: PsiElement

    /**
     * 保存 `identifier`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val identifier: PsiElement?

    /**
     * 保存 `referencedNameElementType`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val referencedNameElementType: IElementType
}

/**
 * 提供 `getTypeArguments` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun CjSimpleNameExpression.getTypeArguments(): List<CjTypeProjection> {
    return when (this) {
        is CjNameReferenceExpression -> typeArguments
        is CjNameBasicReferenceExpression -> typeArguments
        else -> emptyList()
    }
}

/**
 * 提供 `getTypeArgumentList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun CjSimpleNameExpression.getTypeArgumentList(): CjTypeArgumentList? {
    return when (this) {
        is CjNameReferenceExpression -> typeArgumentList
        is CjNameBasicReferenceExpression -> typeArgumentList
        else -> null
    }
}

/**
 * 表示 `CjSimpleNameExpressionImpl`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
abstract class CjSimpleNameExpressionImpl(node: ASTNode) : CjExpressionImpl(node), CjSimpleNameExpression {
    /**
     * 暴露 `identifier`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val identifier get(): PsiElement? = findChildByType(CjTokens.IDENTIFIER)

    /**
     * 暴露 `referencedNameElementType`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedNameElementType get() = getReferencedNameElementTypeImpl(this)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitSimpleNameExpression(this, data)
    }

    /**
     * 暴露 `referencedNameAsName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedNameAsName get() = getReferencedNameAsNameImpl(this)

    /**
     * 暴露 `referencedName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedName get() = getReferencedNameImpl(this)

    companion object {
        fun getReferencedNameElementTypeImpl(expression: CjSimpleNameExpression): IElementType {
            return expression.referencedNameElement.node!!.elementType
        }

        fun getReferencedNameAsNameImpl(expresssion: CjSimpleNameExpression): Name {
            val name = expresssion.referencedName
            return Name.identifier(name)
        }

        fun getReferencedNameImpl(expression: CjSimpleNameExpression): String {
            val text = expression.referencedNameElement.node!!.text
            return CjPsiUtil.unquoteIdentifierOrFieldReference(text)
        }
    }
}
