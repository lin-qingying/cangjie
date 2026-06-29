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
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

/**
 * 表示 `CjUnaryExpression`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
abstract class CjUnaryExpression(node: ASTNode) : CjExpressionImpl(node), CjOperationExpression {

    /**
     * 保存 `baseExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    @get:IfNotParsed
    open val baseExpression: CjExpression? get() = PsiTreeUtil.getPrevSiblingOfType(operationReference, CjExpression::class.java)

    /**
     * 暴露 `operationReference`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val operationReference: CjSimpleNameExpression
        get() = findChildByType(CjNodeTypes.OPERATION_REFERENCE)!!
    /**
     * 保存 `operationToken`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val operationToken: IElementType
        get() = operationReference.referencedNameElementType
}
