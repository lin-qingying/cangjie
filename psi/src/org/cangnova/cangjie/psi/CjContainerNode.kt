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
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

/**
 * 表示 `CjContainerNode`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
open class CjContainerNode(node: ASTNode) : CjElementImpl(node) {
    /**
     * 提供 `findChildByClass` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    public override fun <T> findChildByClass(aClass: Class<T>): T? {
        return super.findChildByClass(aClass)
    }

    /**
     * 提供 `findChildByType` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    public override fun <T : PsiElement> findChildByType(type: IElementType): T? {
        return super.findChildByType(type)
    }

    /**
     * 保存 `expression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val expression get() = findChildByClass(CjExpression::class.java)
}
