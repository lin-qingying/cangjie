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

import com.intellij.lang.ASTNode
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.lang.CangJieLanguage
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import java.lang.reflect.Constructor

/**
 * 表示 `CjNodeType`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
open class CjNodeType(
    @NotNull @NonNls debugName: String,
    psiClass: Class<out CjElement>?
) : IElementType(debugName, CangJieLanguage) {
    /**
     * 保存 `myPsiFactory` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val myPsiFactory: Constructor<out CjElement>? = try {
        psiClass?.getConstructor(ASTNode::class.java)
    } catch (_: NoSuchMethodException) {
        throw RuntimeException("Must have a constructor with ASTNode")
    }

    /**
     * 表示 `CjLeftBoundNodeType`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
     */
    class CjLeftBoundNodeType(
        @NotNull @NonNls debugName: String,
        psiClass: Class<out CjElement>?
    ) : CjNodeType(debugName, psiClass) {
        /**
         * 实现 `isLeftBound` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun isLeftBound(): Boolean = true
    }

    /**
     * 提供 `createPsi` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun createPsi(node: ASTNode): CjElement {
        assert(node.elementType === this)

        return try {
            myPsiFactory?.newInstance(node) ?: CjElementImpl(node)
        } catch (e: Exception) {
            throw RuntimeException("Error creating psi element for node", e)
        }
    }
}
