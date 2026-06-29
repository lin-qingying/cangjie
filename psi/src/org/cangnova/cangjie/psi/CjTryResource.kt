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

/**
 * 表示 `CjTryResource`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjTryResource(node: ASTNode) : CjElementImpl(node) {

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitTryResource(this, data)
    }

    /**
     * 保存 `parameter`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    @get:IfNotParsed
    val parameter: CjParameter?
        get() {

            return findChildByType(CjNodeTypes.VALUE_PARAMETER)
        }

    /**
     * 保存 `expression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val expression: CjExpression? get() {

        children.forEach {
            if (it is CjExpression && it !is CjParameter) return it
        }

        return null
    }
}

/**
 * 表示 `CjTryResourceList`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjTryResourceList(node: ASTNode) : CjElementImpl(node) {

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitTryResourceList(this, data)
    }

    /**
     * 保存 `resources`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val resources: List<CjTryResource> get() = findChildrenByType(CjNodeTypes.TRY_RESOURCE)
}
