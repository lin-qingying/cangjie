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
import org.cangnova.cangjie.psi.psiUtil.startOffset
import org.cangnova.cangjie.psi.stubs.CangJieImportAliasStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.LocalSearchScope

/**
 * 表示 `CjImportAlias`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjImportAlias : CjElementImplStub<CangJieImportAliasStub>, PsiNameIdentifierOwner {
    @Suppress("unused")
    constructor(node: ASTNode) : super(node)

    @Suppress("unused")
    constructor(stub: CangJieImportAliasStub) : super(stub, CjStubElementTypes.IMPORT_ALIAS)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitImportAlias(this, data)
    }

    /**
     * 保存 `importDirective`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val importDirective: CjImportInfo?
        get() = parent as? CjImportInfo

    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName() = stub?.getName() ?: nameIdentifier?.text

    /**
     * 实现 `setName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setName(name: String): PsiElement {
        nameIdentifier?.replace(CjPsiFactory(project).createNameIdentifier(name))
        return this
    }

    /**
     * 实现 `getNameIdentifier` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getNameIdentifier(): PsiElement? = findChildByType(CjTokens.IDENTIFIER)

    /**
     * 实现 `getTextOffset` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getTextOffset() = nameIdentifier?.textOffset ?: startOffset

    /**
     * 实现 `getUseScope` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getUseScope() = LocalSearchScope(containingFile)
}
