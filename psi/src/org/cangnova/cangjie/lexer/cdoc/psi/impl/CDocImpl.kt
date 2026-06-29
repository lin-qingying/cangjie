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

import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.psiUtil.getChildOfType
import org.cangnova.cangjie.psi.psiUtil.getChildrenOfType
import org.cangnova.cangjie.psi.psiUtil.getParentOfType
import org.cangnova.cangjie.utils.toLowerCaseAsciiOnly
import com.intellij.lang.Language
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.tree.IElementType

/**
 * 表示 `CDocImpl`，承载仓颉词法与文档注释中的语法节点、索引桩或辅助模型。
 */
class CDocImpl(buffer: CharSequence?) : LazyParseablePsiElement(CDocTokens.CDOC, buffer), CDoc {

    /**
     * 实现 `getLanguage` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getLanguage(): Language = CangJieLanguage

    /**
     * 实现 `toString` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String = node.elementType.toString()

    /**
     * 实现 `getTokenType` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getTokenType(): IElementType = CjTokens.DOC_COMMENT

    /**
     * 实现 `getOwner` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getOwner(): CjDeclaration? = getParentOfType(true)

    /**
     * 实现 `getDefaultSection` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getDefaultSection(): CDocSection = getChildOfType()!!

    /**
     * 实现 `getAllSections` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getAllSections(): List<CDocSection> =
        getChildrenOfType<CDocSection>().toList()

    /**
     * 实现 `findSectionByName` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun findSectionByName(name: String): CDocSection? =
        getChildrenOfType<CDocSection>().firstOrNull { it.name == name }

    /**
     * 实现 `findSectionByTag` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun findSectionByTag(tag: CDocKnownTag): CDocSection? =
        findSectionByName(tag.name.toLowerCaseAsciiOnly())

    /**
     * 实现 `findSectionByTag` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun findSectionByTag(tag: CDocKnownTag, subjectName: String): CDocSection? =
        getChildrenOfType<CDocSection>().firstOrNull {
            it.name == tag.name.toLowerCaseAsciiOnly() && it.getSubjectName() == subjectName
        }
}
