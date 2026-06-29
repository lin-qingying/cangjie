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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.stubs.CangJiePropertyStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 表示 `CjProperty`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
open class CjProperty : CjTypeParameterListOwnerStub<CangJiePropertyStub>, CjVariableDeclaration {
    companion object {
        private val LET_VAR_TOKEN_SET =
            TokenSet.create(CjTokens.PROP_KEYWORD, CjTokens.MUT_KEYWORD, CjTokens.CONST_KEYWORD)
        val LOG: Logger = Logger.getInstance(
            CjProperty::class.java,
        )
    }
    /**
     * 保存 `isLocal`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isLocal: Boolean
        get() = !isMember

    /**
     * 保存 `isMember`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isMember: Boolean
        get() {
            val parent = parent
            return parent is CjTypeStatement || parent is CjAbstractClassBody
        }
    /**
     * 暴露 `isStatic`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isStatic: Boolean
        get() = hasModifier(CjTokens.STATIC_KEYWORD)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitProperty(this, data)
    }

    /**
     * 提供 `hasBody` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun hasBody(): Boolean {
//        if (hasDelegateExpressionOrInitializer()) return true

        if (getter != null && getter!!.hasBody()) {
            return true
        }

        if (setter != null && setter!!.hasBody()) {
            return true
        }
        return false
    }

    /**
     * 暴露 `typeReference`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeReference: CjTypeReference?
        get() {
            val stub = stub
            if (stub != null) {
                val typeReferences = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)
                if (typeReferences.isEmpty()) {
                    LOG.error("Invalid stub structure built for property: fqName=${stub?.getFqName()}, typeReferences=0")
                    return null
                }
                return typeReferences[0]
            }
            return getTypeReference(this)
        }
    /**
     * 实现 `setTypeReference` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, nameIdentifier, typeRef)
    }

    /**
     * 暴露 `colon`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)

    /**
     * 暴露 `initializer`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val initializer: CjExpression?
        get() = null

    /**
     * 实现 `hasInitializer` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasInitializer(): Boolean {
        val stub: CangJiePropertyStub? = stub
//        if (stub != null) {
//            return stub.hasInitializer()
//        }

        return initializer != null
    }

    /**
     * 暴露 `letOrVarKeyword`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val letOrVarKeyword: PsiElement?
        get() {
            val element =
                checkNotNull(findChildByType(LET_VAR_TOKEN_SET)) { "Let or var should always exist for property" + this.text }
            return element
        }

    constructor(stub: CangJiePropertyStub) : super(stub, CjStubElementTypes.PROPERTY)
    constructor(node: ASTNode) : super(node)

    /**
     * 暴露 `isVar`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isVar: Boolean
        get() = hasModifier(CjTokens.MUT_KEYWORD)

    /**
     * 实现 `getNameIdentifier` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getNameIdentifier(): PsiElement? {
        val propKeyword = node.findChildByType(CjTokens.PROP_KEYWORD) ?: return null
        var child = propKeyword.treeNext
        while (child?.psi is PsiWhiteSpace || child?.psi is PsiComment) {
            child = child?.treeNext
        }
        return when (child?.elementType) {
            CjTokens.IDENTIFIER, CjNodeTypes.OPERATION_NAME -> child.psi

            else -> null
        }
    }

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String = super.toString() + ": " + name
    /**
     * 保存 `receiverTypeRefByTree` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val receiverTypeRefByTree: CjTypeReference?
        get() {
            val parent = this.getStrictParentOfType<CjExtend>()

            return parent?.receiverTypeReceiver
        }

    /**
     * 暴露 `valueParameterList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameterList: CjParameterList? = null
    /**
     * 暴露 `valueParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameters: List<CjParameter> = emptyList()

    /**
     * 保存 `body`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val body: CjPropertyBody?
        get() = getStubOrPsiChild(CjStubElementTypes.PROPERTY_BODY)

    /**
     * 保存 `accessors`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val accessors: List<CjPropertyAccessor>
        get() = body?.accessors ?: emptyList()

    /**
     * 保存 `getter`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val getter: CjPropertyAccessor?
        get() {
            for (accessor in accessors) {
                if (accessor.isGetter) return accessor
            }
            return null
        }

    /**
     * 保存 `setter`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val setter: CjPropertyAccessor?
        get() {
            for (accessor in accessors)
                if (accessor.isSetter) return accessor
            return null
        }
}
