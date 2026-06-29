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
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.stubs.CangJieNamedFunctionStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.cangnova.cangjie.psi.stubs.CangJieFunctionStub

/**
 * 表示 `CjFunctionImpl`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
abstract class CjFunctionImpl<Stub: CangJieFunctionStub<F>,F: CjFunction> :
    CjTypeParameterListOwnerStub<Stub>,
    CjFunction,
    CjDeclarationWithInitializer {
    constructor(node: ASTNode) : super(node)

    constructor(stub: Stub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    /**
     * 暴露 `valueParameterList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameterList: CjParameterList?
        get() = getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST)

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        return node.elementType.toString()
    }

//    是否需要推断返回值类型
    /**
     * 保存 `isInferReturnType`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    open val isInferReturnType: Boolean get() = typeReference == null
    /**
     * 提供 `hasTypeParameterListBeforeFunctionName` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun hasTypeParameterListBeforeFunctionName(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasTypeParameterListBeforeFunctionName()
        }
        return hasTypeParameterListBeforeFunctionNameByTree()
    }

    /**
     * 执行 `hasTypeParameterListBeforeFunctionNameByTree` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun hasTypeParameterListBeforeFunctionNameByTree(): Boolean {
        val typeParameterList = typeParameterList ?: return false
        val nameIdentifier = nameIdentifier ?: return true
        return nameIdentifier.textOffset > typeParameterList.textOffset
    }


    /**
     * 保存 `originalTypeParameterList`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val originalTypeParameterList: CjTypeParameterList? get() = super.typeParameterList

    /**
     * 暴露 `typeParameterList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeParameterList: CjTypeParameterList?
        get() {

            val superTypeParameterList = super.typeParameterList

            if (superTypeParameterList != null) return superTypeParameterList

            return null
        }
    /**
     * 保存 `receiverTypeRefByTree` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val receiverTypeRefByTree: CjTypeReference?
        get() {
            val parent = this.getStrictParentOfType<CjExtend>()

            return parent?.receiverTypeReceiver
        }

    /**
     * 暴露 `typeReference`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeReference: CjTypeReference?
        get() {
            val stub = stub
            if (stub != null) {
                val typeReferences =
                    getStubOrPsiChildrenAsList(
                        CjStubElementTypes.TYPE_REFERENCE,
                    )
                // Extension members declared inside `extend` blocks do not carry
                // an extra receiver type reference in function PSI children.
                val returnTypeIndex = 0
                if (returnTypeIndex >= typeReferences.size) {
                    return null
                }
                return typeReferences[returnTypeIndex]
            }
            return getTypeReference(this)
        }

    /**
     * 实现 `setTypeReference` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, valueParameterList, typeRef)
    }

    /**
     * 暴露 `colon`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)
    /**
     * 暴露 `bodyExpression`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val bodyExpression: CjExpression?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasBody()) {
                    return null
                }
                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            return findChildByClass(CjExpression::class.java)
        }

    /**
     * 暴露 `keyword`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val keyword: PsiElement?
        get() = findChildByType(CjTokens.FUNC_KEYWORD)
    /**
     * 暴露 `equalsToken`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val equalsToken: PsiElement?
        get() = findChildByType(CjTokens.EQ)
    /**
     * 暴露 `bodyBlockExpression`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val bodyBlockExpression: CjBlockExpression?
        get() {

            val stub = stub
            if (stub != null) {
                if (!(stub.hasBlockBody() && stub.hasBody())) {
                    return null
                }
                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            val bodyExpression = findChildByClass(
                CjExpression::class.java,
            )
            if (bodyExpression is CjBlockExpression) {
                return bodyExpression
            }

            return null
        }

    /**
     * 实现 `hasBlockBody` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBlockBody(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasBlockBody()
        }
        return equalsToken == null
    }

    /**
     * 实现 `hasBody` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBody(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasBody()
        }
        return bodyExpression != null
    }

    /**
     * 实现 `hasDeclaredReturnType` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasDeclaredReturnType(): Boolean {
        return false
    }

    /**
     * 暴露 `valueParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameters: List<CjParameter>
        get() {

            val list = valueParameterList
            return list?.parameters ?: emptyList()
        }

    /**
     * 暴露 `isLocal`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isLocal: Boolean
        get() {
            val parent = parent
            return !(parent is CjFile || parent is CjAbstractClassBody)
        }
    /**
     * 暴露 `isUnsafe`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isUnsafe
        get() = hasModifier(CjTokens.UNSAFE_KEYWORD)
    /**
     * 暴露 `isStatic`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isStatic: Boolean
        get() = hasModifier(CjTokens.STATIC_KEYWORD)
    /**
     * 暴露 `isOperator`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isOperator: Boolean
        get() = hasModifier(CjTokens.OPERATOR_KEYWORD)

    /**
     * 暴露 `initializer`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val initializer: CjExpression?
        //    public bool mayHaveContract() {
        get() = PsiTreeUtil.getNextSiblingOfType(
            equalsToken,
            CjExpression::class.java,
        )

    override fun hasInitializer(): Boolean {
        return initializer != null
    }

    open val isTopLevel: Boolean
        //    @Override
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isTopLevel()
            }

            return parent is CjFile
        }

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement? {
        return super.setName(name)
    }
}
