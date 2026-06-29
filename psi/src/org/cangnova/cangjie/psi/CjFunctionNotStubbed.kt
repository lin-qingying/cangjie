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
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

/**
 * 表示 `CjFunctionNotStubbed`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
@Suppress("deprecation")
abstract class CjFunctionNotStubbed(node: ASTNode) :
    CjTypeParameterListOwnerNotStubbed(node),
    CjFunction {
    /**
     * 暴露 `valueParameterList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameterList: CjParameterList?
        get() = findChildByType(CjNodeTypes.VALUE_PARAMETER_LIST)

    /**
     * 暴露 `valueParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameters: List<CjParameter>
        get() {
            val list = valueParameterList
            return list?.parameters ?: emptyList()
        }

    /**
     * 暴露 `bodyExpression`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val bodyExpression: CjExpression?
        get() {
            return findChildByClass(CjExpression::class.java)
        }

    /**
     * 实现 `hasDeclaredReturnType` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasDeclaredReturnType(): Boolean {
        return false
    }



    /**
     * 暴露 `typeReference`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeReference: CjTypeReference?
        get() = null

    /**
     * 实现 `setTypeReference` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        if (typeRef == null) return null
        throw IllegalStateException("Lambda expressions can't have type reference")
    }

    /**
     * 暴露 `colon`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val colon: PsiElement?
        get() = null

    /**
     * 暴露 `isLocal`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isLocal: Boolean
        get() {
            val parent = parent
            return !(parent is CjFile || parent is CjAbstractClassBody)
        }
}
