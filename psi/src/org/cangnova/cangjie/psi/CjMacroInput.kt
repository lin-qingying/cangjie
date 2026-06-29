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

import org.cangnova.cangjie.parsing.CangJieExpressionParsing
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * 表示 `CjMacroInput`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjMacroInput(node: ASTNode) : CjExpressionImpl(node) {

    /**
     * 保存 `declarations`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val declarations: CjDeclaration?
        get() {
            return PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java).firstOrNull()
        }
    /**
     * 保存 `tokens`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val tokens: List<PsiElement>
        get() = findChildByType<CjQuoteTokens>(CjNodeTypes.QUOTE_TOKENS)?.tokens ?: emptyList()
}

/**
 * 表示 `CjQuoteParameters`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjQuoteParameters(node: ASTNode) : CjExpressionImpl(node)
/**
 * 表示 `CjMacroAttr`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjMacroAttr(node: ASTNode) : CjExpressionImpl(node){
    /**
     * 保存 `tokens`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val tokens: List<PsiElement>
        get() = findChildByType<CjQuoteTokens>(CjNodeTypes.QUOTE_TOKENS)?.tokens ?: emptyList()

}
/**
 * 表示 `CjQuoteTokens`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjQuoteTokens(node: ASTNode) : CjExpressionImpl(node) {

    /**
     * 保存 `tokens`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val tokens: List<PsiElement> get() = findChildrenByType(CangJieExpressionParsing.QUOTE_TOKENS)
}
