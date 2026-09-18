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

import org.cangnova.cangjie.lexer.CjSingleValueToken
import org.cangnova.cangjie.name.OperatorConventions
import org.cangnova.cangjie.parsing.CangJieExpressionParsing
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.CangJieOperationReferenceStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.tree.IElementType

/**
 * 表示 `CjOperationReferenceExpression`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjOperationReferenceExpression : CjExpressionImplStub<CangJieOperationReferenceStub>, CjSimpleNameExpression {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieOperationReferenceStub) : super(stub, CjStubElementTypes.OPERATION_REFERENCE)

    override val identifier: PsiElement? get() = null
    override val referencedName: String
        get() = stub?.operationToken?.value ?: CjSimpleNameExpressionImpl.getReferencedNameImpl(this)
    override val referencedNameAsName: Name get() = Name.identifier(referencedName)
    override val referencedNameElementType: IElementType
        get() = stub?.operationToken ?: CjSimpleNameExpressionImpl.getReferencedNameElementTypeImpl(this)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? =
        visitor.visitSimpleNameExpression(this, data)

    /**
     * 暴露 `referencedNameElement`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedNameElement get(): PsiElement {
        if (stub != null) return this
        return CangJieExpressionParsing.ALL_OPERATIONS?.let { findChildByType<PsiElement>(it) } ?: this
    }

    /**
     * 提供 `isConventionOperator` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isConventionOperator(): Boolean {
        val tokenType = operationSignTokenType ?: return false
        return OperatorConventions.getNameForOperationSymbol(tokenType) != null
    }
    /**
     * 保存 `operationSignTokenType`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val operationSignTokenType: CjSingleValueToken?
        get() = stub?.operationToken ?: (node.firstChildNode?.elementType as? CjSingleValueToken)
}
