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

import org.cangnova.cangjie.name.*
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjModifierKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName.Companion.fromString
import org.cangnova.cangjie.name.Name.Companion.identifier
import org.cangnova.cangjie.psi.psiUtil.CjStubbedPsiUtil
import org.cangnova.cangjie.psi.psiUtil.getQualifiedElementSelector
import org.cangnova.cangjie.psi.stubs.CangJiePackageDirectiveStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.CjTokenSets
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import java.util.*

/**
 * 表示 `CjPackageDirective`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjPackageDirective : CjDeclarationStub<CangJiePackageDirectiveStub> {
    /**
     * 保存 `qualifiedNameCache` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private var qualifiedNameCache: String? = null

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePackageDirectiveStub) : super(stub, CjStubElementTypes.PACKAGE_DIRECTIVE)

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        return super.toString()
    }

    /**
     * 保存 `packageNameExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val packageNameExpression
        get() = CjStubbedPsiUtil.getStubOrPsiChild(
            this,
            CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS,
            CjExpression.ARRAY_FACTORY,
        )


    /**
     * 保存 `packageNames`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val packageNames: List<CjSimpleNameExpression>
        get() {
            var nameExpression = this.packageNameExpression ?: return mutableListOf<CjSimpleNameExpression>()

            val packageNames: MutableList<CjSimpleNameExpression> =
                ArrayList<CjSimpleNameExpression>()
            while (nameExpression is CjQualifiedExpression) {
                val selector = nameExpression.selectorExpression
                if (selector is CjSimpleNameExpression) {
                    packageNames.add(selector)
                }

                nameExpression = nameExpression.receiverExpression
            }

            if (nameExpression is CjSimpleNameExpression) {
                packageNames.add(nameExpression)
            }

            packageNames.reverse()

            return packageNames
        }

    /**
     * 保存 `lastReferenceExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val lastReferenceExpression: CjSimpleNameExpression?
        get() {
            val nameExpression = this.packageNameExpression ?: return null

            return nameExpression.getQualifiedElementSelector() as CjSimpleNameExpression?
        }

    /**
     * 保存 `nameIdentifier`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val nameIdentifier: PsiElement?
        get() {
            val lastPart = this.lastReferenceExpression
            return lastPart?.identifier
        }

    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String {
        val nameIdentifier = this.nameIdentifier
        return if (nameIdentifier == null) "" else nameIdentifier.text
    }

    /**
     * 实现 `navigate` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun navigate(requestFocus: Boolean) {
        super.navigate(requestFocus)
    }

    /**
     * 实现 `canNavigateToSource` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun canNavigateToSource(): Boolean {
        return super.canNavigateToSource()
    }

    /**
     * 实现 `canNavigate` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun canNavigate(): Boolean {
        return super.canNavigate()
    }

    /**
     * 执行 `getModifier` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun getModifier(tokenType: CjKeywordToken): PsiElement? {
        return findChildByType(tokenType)
    }

    /**
     * 提供 `hasModifier` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun hasModifier(tokenType: CjModifierKeywordToken): Boolean {
//        CangJieImportDirectiveItemStub stub = getStub();
//        if (stub != null) {
//            return stub.getModifierVisibility(tokenType);
//        }
        return getModifier(tokenType) != null
    }

    /**
     * 保存 `isMacroPackage`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isMacroPackage: Boolean
        get() = stub?.isMacroPackage ?: (findChildByType<PsiElement>(CjTokens.MACRO_KEYWORD) != null)

    /**
     * 保存 `nameAsName`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val nameAsName: Name
        get() {
            val nameIdentifier = this.nameIdentifier
            return if (nameIdentifier == null) {
                SpecialNames.ROOT_PACKAGE
            } else {
                identifier(
                    nameIdentifier.text,
                )
            }
        }

    /**
     * 保存 `isRoot`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isRoot: Boolean
        get() = getName().isEmpty()

    /**
     * 保存 `fqName`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    var fqName: FqName = FqName.ROOT
        get() {
            val qualifiedName = this.qualifiedName
            return if (qualifiedName.isEmpty()) {
                FqName.ROOT
            } else {
                fromString(
                    qualifiedName,
                )
            }
        }
        set(fqName) {
            if (fqName.isRoot) {
                if (!field.isRoot) {
                    replace(
                        Objects.requireNonNull<CjPackageDirective>(
                            CjPsiFactory(project).createFile(
                                "",
                            ).packageDirective,
                        ),
                    )
                }
                return
            }

            val psiFactory = CjPsiFactory(project)
            val newExpression: PsiElement = psiFactory.createExpression(fqName.asString())
            val currentExpression = this.packageNameExpression
            if (currentExpression != null) {
                currentExpression.replace(newExpression)
                return
            }

            val keyword = this.packageKeyword
            if (keyword != null) {
                addAfter(newExpression, keyword)
                addAfter(psiFactory.createWhiteSpace(), keyword)
                return
            }

            replace(psiFactory.createPackageDirective(fqName))
        }

    /**
     * 提供 `getFqName` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getFqName(nameExpression: CjSimpleNameExpression?): FqName {
        return FqName(getQualifiedNameOf(nameExpression))
    }

    /**
     * 保存 `qualifiedName`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val qualifiedName: String
        get() {
            if (qualifiedNameCache == null) {
                qualifiedNameCache = getQualifiedNameOf(null)
            }

            return qualifiedNameCache!!
        }

    /**
     * 执行 `getQualifiedNameOf` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun getQualifiedNameOf(nameExpression: CjSimpleNameExpression?): String {
        val builder = StringBuilder()
        for (e in this.packageNames) {
            if (builder.isNotEmpty()) {
                builder.append(".")
            }
            builder.append(e.referencedName)

            if (e === nameExpression) break
        }
        return builder.toString()
    }

    /**
     * 保存 `packageKeyword`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val packageKeyword
        get() = findChildByType<PsiElement>(CjTokens.PACKAGE_KEYWORD)

    /**
     * 实现 `subtreeChanged` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun subtreeChanged() {
        qualifiedNameCache = null
    }

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPackageDirective(this, data)
    }
}
