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
import org.cangnova.cangjie.psi.stubs.CangJieUserTypeStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.google.common.collect.Lists
import com.intellij.lang.ASTNode

/**
 * 表示 `CjUserType`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjUserType : CjElementImplStub<CangJieUserTypeStub>, CjTypeElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieUserTypeStub) : super(stub, CjStubElementTypes.USER_TYPE)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitUserType(this, data)
    }

    /**
     * 保存 `typeArgumentList`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeArgumentList: CjTypeArgumentList?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_ARGUMENT_LIST)

    /**
     * 保存 `typeArguments`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeArguments: List<CjTypeProjection>
        get() {
            val typeArgumentList = typeArgumentList
            return typeArgumentList?.arguments ?: emptyList()
        }

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        return node.elementType.toString()
    }

    /**
     * 暴露 `typeArgumentsAsTypes`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeArgumentsAsTypes: List<CjTypeReference>
        get() {
            val result: MutableList<CjTypeReference> =
                Lists.newArrayList()
            for (projection in typeArguments) {
                projection.typeReference?.let { result.add(it) }
            }
            return result
        }

    /**
     * 保存 `referenceExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    @get:IfNotParsed
    val referenceExpression: CjSimpleNameExpression?
        get() = getStubOrPsiChild(CjStubElementTypes.REFERENCE_EXPRESSION)

    /**
     * 保存 `qualifier`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val qualifier: CjUserType?
        get() = getStubOrPsiChild(CjStubElementTypes.USER_TYPE)

    /**
     * 保留除该USER_TYPE以外指定数量的psi元素
     * 例如 USER_TYPE = a.b.c; size = 1
     * 保留 b.c
     *
     * @param size
     */
    fun deleteQualifier(size: Int) {
        if (size <= 0) {
            deleteQualifier()
        }
        val qualifier = qualifier

        qualifier?.deleteQualifier(size - 1)
    }

    /**
     * 提供 `deleteQualifier` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun deleteQualifier() {
        val qualifier = checkNotNull(qualifier)
        val dot = checkNotNull(findChildByType(CjTokens.DOT))
        qualifier.delete()
        dot.delete()
    }

    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String? {
        return referencedName
    }
    /**
     * 保存 `referencedName`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val referencedName: String?
        get() {
            val referenceExpression = referenceExpression
            return referenceExpression?.referencedName
        }
}
