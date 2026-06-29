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
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.psiUtil.astReplace
import org.cangnova.cangjie.psi.psiUtil.quoteIfNeeded
import org.cangnova.cangjie.psi.stubs.CangJieFieldStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorName

/**
 * 类成员字段 PSI 元素
 *
 * 表示类/结构体/接口中的 let/var/const 声明。
 * 与 CjPatternVariable 不同，CjFieldVariable 不支持模式匹配，只有简单的标识符名称。
 *
 * 示例:
 * ```cangjie
 * class Person {
 *     let name: String           // 不可变字段
 *     var age: Int64             // 可变字段
 *     const MAX_AGE: Int64 = 150 // 常量字段
 * }
 * ```
 *
 * @see CjVariable
 * @see CjPatternVariable
 */
class CjFieldVariable : CjVariable<CangJieFieldStub>  {

    constructor(stub: CangJieFieldStub) : super(stub, CjStubElementTypes.FIELD)
    constructor(node: ASTNode) : super(node)

    companion object {
        private val LOG = Logger.getInstance(CjFieldVariable::class.java)

        private val LET_VAR_CONST_TOKEN_SET = TokenSet.create(
            CjTokens.LET_KEYWORD,
            CjTokens.VAR_KEYWORD,
            CjTokens.CONST_KEYWORD
        )
    }

    /**
     * 暴露 `valueParameterList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameterList: CjParameterList?
        get() = null

    /**
     * 暴露 `valueParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameters: List<CjParameter>
        get() = emptyList()

    /**
     * 保存 `equalsToken`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val equalsToken: PsiElement?
        get() = findChildByType(CjTokens.EQ)

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        return super.toString() + ": " + name
    }

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitFieldVariable(this, data)
    }
    //需要实现textOffset
    /**
     * 实现 `getTextOffset` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getTextOffset(): Int {
        val identifier = nameIdentifier
        return identifier?.textRange?.startOffset ?: textRange.startOffset
    }


    /**
     * 暴露 `typeReference`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeReference: CjTypeReference?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasReturnTypeRef()) {
                    return null
                } else {
                    val typeReferences = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)
                    if (typeReferences.isEmpty()) {
                        LOG.error(
                            """
                            Invalid stub structure built for field:
                            $text
                            """.trimIndent(),
                        )
                        return null
                    }
                    return typeReferences[0]
                }
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
     * 暴露 `isVar`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isVar: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isVar()
            }
            return node.findChildByType(CjTokens.VAR_KEYWORD) != null
        }

    /**
     * 保存 `isConst`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isConst: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isConst()
            }
            return node.findChildByType(CjTokens.CONST_KEYWORD) != null
        }

    /**
     * 暴露 `isStatic`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isStatic: Boolean
        get() = hasModifier(CjTokens.STATIC_KEYWORD)

    /**
     * 暴露 `letOrVarKeyword`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val letOrVarKeyword: PsiElement
        get() {
            val element = checkNotNull(findChildByType(LET_VAR_CONST_TOKEN_SET)) {
                "Let, var, or const should always exist for field: " + this.text
            }
            return element
        }

    /**
     * 暴露 `initializer`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val initializer: CjExpression?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasInitializer()) {
                    return null
                }
                if (containingCjFile.isCompiled) {
                    return null
                }
            }
            return PsiTreeUtil.getNextSiblingOfType(
                findChildByType(CjTokens.EQ),
                CjExpression::class.java,
            )
        }

    /**
     * 实现 `hasInitializer` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasInitializer(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasInitializer()
        }
        return initializer != null
    }

    /**
     * 获取所属的类型声明（class/struct/interface/enum）
     */
    val containingTypeStatement: CjTypeStatement?
        get() = getStrictParentOfType<CjTypeStatement>()

    // ========== CjNamedDeclaration 实现 ==========

    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String? {
        val stub = stub
        if (stub != null) {
            return stub.name
        }
        val identifier = nameIdentifier
        if (identifier != null) {
            val text = identifier.text
            return if (text != null) CjPsiUtil.unquoteIdentifier(text) else null
        }
        return null
    }

    /**
     * 实现 `getNameIdentifier` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getNameIdentifier(): PsiElement? {
        return findChildByType(CjTokens.IDENTIFIER)
    }

    /**
     * 实现 `setName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setName(name: @NlsSafe String): PsiElement? {
        val identifier = nameIdentifier ?: return null
        val newIdentifier = CjPsiFactory(project).createNameIdentifierIfPossible(name.quoteIfNeeded())
        if (newIdentifier != null) {
            identifier.astReplace(newIdentifier)
        } else {
            identifier.delete()
        }
        return this
    }

    /**
     * 暴露 `nameAsName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val nameAsName: Name?
        get() = this.name?.asOperatorName()

    /**
     * 暴露 `nameAsSafeName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val nameAsSafeName: Name
        get() = CjPsiUtil.safeName(name)

    /**
     * 暴露 `fqName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val fqName: FqName?
        get() {
            val stub = getStub()
            if (stub != null) {
                return stub.getFqName()
            }
            return CjNamedDeclarationUtil.getFQName(this)
        }

    // ========== CjTypeParameterListOwner 实现 ==========
    // 字段不支持类型参数，返回 null/空列表

    /**
     * 暴露 `typeParameterList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeParameterList: CjTypeParameterList?
        get() = null

    /**
     * 暴露 `typeConstraintList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeConstraintList: CjTypeConstraintList?
        get() = null

    /**
     * 暴露 `typeConstraints`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeConstraints: List<CjTypeConstraint>
        get() = emptyList()

    /**
     * 暴露 `typeParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeParameters: List<CjTypeParameter>
        get() = emptyList()
}
