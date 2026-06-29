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

import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

/**
 * 表示 `CjSuperTypeCallEntry`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjSuperTypeCallEntry : CjSuperTypeListEntry, CjCallElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<out CjSuperTypeListEntry>) : super(
        stub,
        CjStubElementTypes.SUPER_TYPE_CALL_ENTRY,
    )

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitSuperTypeCallEntry(this, data)
    }

    /**
     * 暴露 `calleeExpression`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val calleeExpression: CjConstructorCalleeExpression get() {
        return getRequiredStubOrPsiChild(CjStubElementTypes.CONSTRUCTOR_CALLEE)
    }

    /**
     * 暴露 `lambdaArguments`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val lambdaArguments: List<CjLambdaArgument> get() {
        return emptyList()
    }

    /**
     * 暴露 `typeArguments`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeArguments: List<CjTypeProjection> get() {
        val typeArgumentList = typeArgumentList ?: return emptyList()
        return typeArgumentList.arguments
    }

    /**
     * 暴露 `typeArgumentList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeArgumentList: CjTypeArgumentList? = null

    /**
     * 暴露 `valueArgumentList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueArgumentList: CjValueArgumentList? get() {
        return findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST)
    }

    /**
     * 暴露 `valueArguments`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueArguments: List<ValueArgument> get() {
        val list = valueArgumentList
        return list?.arguments ?: emptyList<CjValueArgument>()
    }

    /**
     * 暴露 `typeReference`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeReference: CjTypeReference? get() {
        return calleeExpression.typeReference
    }
}
