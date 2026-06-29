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
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

/**
 * 表示 `CjForExpression`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjForExpression(node: ASTNode) : CjLoopExpression(node), CjPatternEntryBlock {
    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitForExpression(this, data)
    }

    /**
     * 保存 `loopParameter`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    @get: IfNotParsed
    val loopParameter: CjParameter?
        get() = findChildByType<PsiElement>(CjNodeTypes.VALUE_PARAMETER) as CjParameter?

    /**
     * 保存 `pattern`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val pattern: CjCasePatternElement?
        get() {

            return findChildByClass(CjCasePatternElement::class.java)
        }
    /**
     * 保存 `patternGuard`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val patternGuard: CjPatternGuard?
        get() {

            return findChildByClass(CjPatternGuard::class.java)
        }

    /**
     * 保存 `loopRange`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    @get: IfNotParsed
    val loopRange: CjExpression?
        get() = findExpressionUnder(CjNodeTypes.LOOP_RANGE)

    /**
     * 保存 `inKeyword`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    @get: IfNotParsed
    val inKeyword: PsiElement?
        get() = findChildByType(CjTokens.IN_KEYWORD)
    /**
     * 保存 `forKeyword`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val forKeyword: PsiElement?
        get() = findChildByType(CjTokens.FOR_KEYWORD)
}
