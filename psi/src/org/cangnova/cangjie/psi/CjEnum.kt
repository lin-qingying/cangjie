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

import org.cangnova.cangjie.psi.stubs.CangJieEnumStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.ENUM_BODY
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.lexer.CjTokens

/**
 * 表示 `CjEnum`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjEnum : CjTypeStatement {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieEnumStub) : super(stub, CjStubElementTypes.ENUM)

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String = node.elementType.toString() + ": " + name

    /**
     * 暴露 `typeName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeName: String
        get() = "enum"

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitEnum(this, data)
    }

    /**
     * 暴露 `body`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val body: CjEnumBody?
        get() {
            return getStubOrPsiChild(ENUM_BODY)
        }

    /**
     * 是否非穷枚举
     */
    val isNonExhaustive: Boolean
        get() {
            val stub = stub as CangJieEnumStub?
            if (stub != null) {
                return stub.isNonExhaustive()
            }
            return body?.isNonExhaustive == true
        }

    /**
     * 保存 `constructor`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val constructor: List<CjEnumConstructor>
        get() {
            return body?.constructor ?: emptyList()
        }
}
