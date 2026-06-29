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

package org.cangnova.cangjie.lexer.cdoc.psi.impl

import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjElementImpl
import org.cangnova.cangjie.psi.psiUtil.getChildOfType
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.name.*

/**
 * 标签主题或链接中限定名称的单个部分。
 */
class CDocName(node: ASTNode) : CjElementImpl(node) {
    /**
     * 提供 `getContainingDoc` 操作，封装仓颉词法与文档注释节点的访问、构造或判断逻辑。
     */
    fun getContainingDoc(): CDoc {
        val Cdoc = getStrictParentOfType<CDoc>()
        return Cdoc ?: throw IllegalStateException("CDOCName must be inside a CDOC")
    }

    /**
     * 提供 `getContainingSection` 操作，封装仓颉词法与文档注释节点的访问、构造或判断逻辑。
     */
    fun getContainingSection(): CDocSection {
        val CDOC = getStrictParentOfType<CDocSection>()
        return CDOC ?: throw IllegalStateException("CDOCName must be inside a CDOCSection")
    }

    /**
     * 提供 `getQualifier` 操作，封装仓颉词法与文档注释节点的访问、构造或判断逻辑。
     */
    fun getQualifier(): CDocName? = getChildOfType()

    /**
     *返回包含名称的元素内的范围(换句话说， 不包括限定符和点的元素的范围(如果存在)。
     */
    fun getNameTextRange(): TextRange {
        val dot = node.findChildByType(CjTokens.DOT)
        val textRange = textRange
        val nameStart = if (dot != null) dot.textRange.endOffset - textRange.startOffset else 0
        return TextRange(nameStart, textRange.length)
    }

    /**
     * 提供 `getNameText` 操作，封装仓颉词法与文档注释节点的访问、构造或判断逻辑。
     */
    fun getNameText(): String = getNameTextRange().substring(text)

    /**
     * 提供 `getQualifiedName` 操作，封装仓颉词法与文档注释节点的访问、构造或判断逻辑。
     */
    fun getQualifiedName(): List<String> {
        val qualifier = getQualifier()
        val nameAsList = listOf(getNameText())
        return if (qualifier != null) qualifier.getQualifiedName() + nameAsList else nameAsList
    }

    /**
     * 提供 `getQualifiedNameAsFqName` 操作，封装仓颉词法与文档注释节点的访问、构造或判断逻辑。
     */
    fun getQualifiedNameAsFqName(): FqName {
        return FqName.fromSegments(getQualifiedName())
    }
}
