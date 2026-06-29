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
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.util.IncorrectOperationException
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.CangJieFinalizerStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 表示 `CjFinalizer`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjFinalizer : CjDeclarationStub<CangJieFinalizerStub>, CjFunction {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieFinalizerStub) : super(
        stub,
        CjStubElementTypes.FINALIZER,
    )

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? = visitor.visitFinalizer(this, data)

    /**
     * 提供 `getContainingTypeStatement` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getContainingTypeStatement() = parent?.parent as CjTypeStatement

    /**
     * 保存 `initKeyword`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val initKeyword: PsiElement?
        get() = findChildByType(CjTokens.INIT_KEYWORD)

    /**
     * 暴露 `isLocal`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isLocal = false

    /**
     * 暴露 `bodyExpression`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val bodyExpression: CjBlockExpression?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasBody()) {
                    return null
                }
                if (getContainingCjFile().isCompiled) {
                    return null
                }
            }
            return findChildByClass(CjBlockExpression::class.java)
        }

    /**
     * 暴露 `valueParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameters: List<CjParameter>
        get() = valueParameterList?.parameters ?: emptyList()

    /**
     * 暴露 `typeReference`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeReference: CjTypeReference? = null

    /**
     * 暴露 `valueParameterList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameterList: CjParameterList?
        get() = getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST)

    /**
     * 实现 `setTypeReference` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setTypeReference(typeRef: CjTypeReference?) =
        throw IncorrectOperationException("setTypeReference to finalizer")

    /**
     * 暴露 `colon`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)

    /**
     * 暴露 `equalsToken`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val equalsToken = null

    /**
     * 实现 `hasBlockBody` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBlockBody() = hasBody()

    /**
     * 实现 `hasBody` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBody(): Boolean {
        stub?.let { return it.hasBody() }
        return bodyExpression != null
    }

    /**
     * 暴露 `typeParameterList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeParameterList: CjTypeParameterList?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_PARAMETER_LIST)

    /**
     * 暴露 `typeConstraintList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeConstraintList: CjTypeConstraintList?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_CONSTRAINT_LIST)

    /**
     * 暴露 `typeConstraints`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeConstraints: List<CjTypeConstraint>
        get() = typeConstraintList?.constraints ?: emptyList()

    /**
     * 暴露 `typeParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeParameters: List<CjTypeParameter>
        get() = typeParameterList?.parameters ?: emptyList()

    /**
     * 实现 `hasDeclaredReturnType` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasDeclaredReturnType() = false

    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String? = getContainingTypeStatement().name

    /**
     * 暴露 `fqName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val fqName: FqName?
        get() = null

    /**
     * 暴露 `nameAsSafeName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val nameAsSafeName: Name
        get() = CjPsiUtil.safeName(name)

    /**
     * 暴露 `nameAsName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val nameAsName: Name?
        get() = nameAsSafeName

    /**
     * 实现 `getNameIdentifier` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getNameIdentifier() = null

    /**
     * 实现 `getIdentifyingElement` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getIdentifyingElement(): PsiElement? = initKeyword

    /**
     * 实现 `setName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setName(name: String): PsiElement =
        throw IncorrectOperationException("setName to finalizer")

    /**
     * 实现 `getPresentation` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)

    /**
     * 提供 `hasInitKeyword` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun hasInitKeyword(): Boolean = stub != null || initKeyword != null

    /**
     * 实现 `getTextOffset` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getTextOffset(): Int {
        return initKeyword?.textOffset
            ?: valueParameterList?.textOffset
            ?: super.getTextOffset()
    }

    /**
     * 实现 `getUseScope` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getUseScope(): SearchScope {
        return getContainingTypeStatement().useScope
    }
}
