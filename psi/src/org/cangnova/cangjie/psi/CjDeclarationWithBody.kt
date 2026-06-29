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

import com.intellij.psi.PsiElement

/**
 * 定义 `CjDeclarationWithBody` 接口，约束仓颉 PSI节点或服务需要暴露的结构能力。
 */
interface CjDeclarationWithBody : CjDeclaration {

    /**
     * 保存 `bodyExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val bodyExpression: CjExpression?

    /**
     * 保存 `equalsToken`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val equalsToken: PsiElement?

    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String?

    /**
     * 提供 `hasBlockBody` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun hasBlockBody(): Boolean

    /**
     * 提供 `hasBody` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun hasBody(): Boolean

    /**
     * 提供 `hasDeclaredReturnType` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun hasDeclaredReturnType(): Boolean

    /**
     * 保存 `valueParameters`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val valueParameters: List<CjParameter>

    /**
     * 保存 `bodyBlockExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val bodyBlockExpression: CjBlockExpression?
        get() {
            val bodyExpression = bodyExpression
            if (bodyExpression is CjBlockExpression) {
                return bodyExpression
            }

            return null
        }
}
