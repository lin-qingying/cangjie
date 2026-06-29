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

import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.CangJieValueArgumentStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.stubs.IStubElementType
import org.cangnova.cangjie.lexer.CjTokens

/**
 * 表示 `CjValueArgument`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
open class CjValueArgument :
    CjElementImplStub<CangJieValueArgumentStub<out CjValueArgument>>,
    ValueArgument {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieValueArgumentStub<CjValueArgument>) : super(stub, CjStubElementTypes.VALUE_ARGUMENT)

    protected constructor(
        stub: CangJieValueArgumentStub<out CjValueArgument>,
        nodeType: IStubElementType<*, *>,
    ) : super(stub, nodeType)

    /**
     * 实现 `getArgumentExpression` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getArgumentExpression(): CjExpression? {
        val stub: CangJiePlaceHolderStub<out CjValueArgument>? = stub
        if (stub != null) {
            val constantExpressions =
                stub.getChildrenByType(CjNodeTypes.CONSTANT_EXPRESSIONS_TYPES, CjExpression.EMPTY_ARRAY)
            if (constantExpressions.isNotEmpty()) {
                return constantExpressions[0]
            }
        }

        var child = node.firstChildNode
        while (child != null) {
            (child.psi as? CjExpression)?.let { return it }
            child = child.treeNext
        }

        return findChildByClass(CjExpression::class.java)
    }

    /**
     * 实现 `getArgumentName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getArgumentName(): ValueArgumentName? {
        return getStubOrPsiChild(CjStubElementTypes.VALUE_ARGUMENT_NAME)
    }

    /**
     * 实现 `isNamed` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isNamed(): Boolean {
        return getArgumentName() != null
    }

    /**
     * 实现 `asElement` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun asElement(): CjValueArgument {
        return this
    }

    /**
     * 实现 `getSpreadElement` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getSpreadElement(): LeafPsiElement? {
        return null
    }

    /**
     * 实现 `isExternal` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isExternal(): Boolean {
        return false
    }

    /**
     * 保存 `isSpread`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isSpread: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isSpread()
            }

            return getSpreadElement() != null
        }

    /**
     * 是否有 `inout` 修饰。
     *
     * 仓颉语法：`cFunc(inout x)` 中 `inout` 关键字修饰函数调用参数，
     * 表示该参数按 inout（可写引用）方式传递给 foreign/CFunc 函数。
     */
    val isInout: Boolean
        get() = findChildByType<LeafPsiElement>(CjTokens.INOUT_KEYWORD) != null

    /**
     * 获取 `inout` 关键字的 PSI 节点（用于诊断定位）。
     */
    fun getInoutKeyword(): LeafPsiElement? =
        findChildByType(CjTokens.INOUT_KEYWORD)
}
