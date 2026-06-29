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
import org.cangnova.cangjie.psi.stubs.CangJieNamedFunctionStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorName
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.name.*


/**
 * 表示 `CjNamedFunction`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
open class CjNamedFunction : CjFunctionImpl<CangJieNamedFunctionStub, CjNamedFunction> {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieNamedFunctionStub) : super(stub, CjStubElementTypes.FUNCTION)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitNamedFunction(this, data)
    }

    /**
     * 实现 `hasTypeParameterListBeforeFunctionName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasTypeParameterListBeforeFunctionName(): Boolean {
        val stub: CangJieNamedFunctionStub? = stub
        if (stub != null) {
            return stub.hasTypeParameterListBeforeFunctionName()
        }
        return hasTypeParameterListBeforeFunctionNameByTree()
    }

    /**
     * 执行 `hasTypeParameterListBeforeFunctionNameByTree` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun hasTypeParameterListBeforeFunctionNameByTree(): Boolean {
        val typeParameterList: CjTypeParameterList = typeParameterList ?: return false
        val nameIdentifier: PsiElement = nameIdentifier ?: return true
        return nameIdentifier.textOffset > typeParameterList.textOffset
    }

    //    当前方法是否是operator set方法
//    set方法规则，最后一个参数是命名参数，并且参数名称是value
    /**
     * 保存 `isSetFunc`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isSetFunc: Boolean
        get() {
            if (!isOperator) return false
            return valueParameters.lastOrNull()?.isNamed == true && valueParameters.last().nameAsName?.asString() == "value"
        }

    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String? {
        return super.getName()
    }

    /**
     * 暴露 `nameAsName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val nameAsName: Name?
        get() = normalizedOperatorName() ?: super.nameAsName

    /**
     * 暴露 `fqName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val fqName: FqName?
        get() = super.fqName

    /**
     * 暴露 `nameAsSafeName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val nameAsSafeName: Name
        get() = normalizedOperatorName() ?: super.nameAsSafeName

    /**
     * 操作符函数的 PSI 名称归一化。
     *
     * `+`/`-` 在 PSI 裸文本中无法区分一元与二元，必须在函数声明上下文按参数个数归一化，
     * 这样 PSI raw CFIR、Analysis API 与 LightTree raw CFIR 使用同一套操作符名称。
     */
    private fun normalizedOperatorName(): Name? {
        if (!isOperator) return null

        val rawName = nameIdentifier?.text ?: name ?: return null
        return when (rawName) {
            "-", OperatorNameConventions.MINUS.asString(), OperatorNameConventions.UNARY_MINUS.asString() ->
                if (valueParameters.isEmpty()) OperatorNameConventions.UNARY_MINUS else OperatorNameConventions.MINUS

            "+", OperatorNameConventions.PLUS.asString(), OperatorNameConventions.UNARY_PLUS.asString() ->
                if (valueParameters.isEmpty()) OperatorNameConventions.UNARY_PLUS else OperatorNameConventions.PLUS

            "[]", OperatorNameConventions.GET.asString(), OperatorNameConventions.SET.asString() ->
                if (isSetFunc) OperatorNameConventions.SET else OperatorNameConventions.GET

            else -> rawName.asOperatorName()
        }
    }

    /**
     * 实现 `hasBlockBody` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBlockBody(): Boolean {
        val stub: CangJieNamedFunctionStub? = stub
        if (stub != null) {
            return stub.hasBlockBody()
        }
        return equalsToken == null
    }

    /**
     * 保存 `funKeyword`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    @get:IfNotParsed
    val funKeyword: PsiElement?
        get() = findChildByType(CjTokens.FUNC_KEYWORD)

    /**
     * 暴露 `equalsToken`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val equalsToken: PsiElement?
        get() = super.equalsToken

    /**
     * 暴露 `initializer`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val initializer: CjExpression?
        get() = PsiTreeUtil.getNextSiblingOfType(
            equalsToken,
            CjExpression::class.java,
        )

    /**
     * 实现 `hasInitializer` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasInitializer(): Boolean {
        return initializer != null
    }

    /**
     * 实现 `getPresentation` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getPresentation(): ItemPresentation? {
        return ItemPresentationProviders.getItemPresentation(this)
    }

    /**
     * 暴露 `valueParameterList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameterList: CjParameterList?
        get() {
            return super.valueParameterList
        }

    /**
     * 暴露 `valueParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameters: List<CjParameter>
        get() {
            val list: CjParameterList? = valueParameterList
            return list?.parameters ?: emptyList()
        }

    /**
     * 暴露 `bodyExpression`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val bodyExpression: CjExpression?
        get() {
            return super.bodyExpression
        }

    /**
     * 暴露 `bodyBlockExpression`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val bodyBlockExpression: CjBlockExpression?
        get() {
            return super.bodyBlockExpression
        }

    /**
     * 实现 `hasBody` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBody(): Boolean {
        val stub: CangJieNamedFunctionStub? = stub
        if (stub != null) {
            return stub.hasBody()
        }
        return bodyBlockExpression != null
    }

    /**
     * 实现 `hasDeclaredReturnType` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasDeclaredReturnType(): Boolean {
        return typeReference != null
    }

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
//        return getNode().getElementType().toString();
        return node.elementType.toString() + ": " + name
    }



    /**
     * 暴露 `typeReference`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeReference: CjTypeReference?
        get() {
            return super.typeReference
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
        get() {
            return super.colon
        }

    /**
     * 保存 `isAnonymous`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isAnonymous: Boolean
        get() {
            return name == null && isLocal
        }

    /**
     * 暴露 `isMut`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isMut: Boolean
        get() = hasModifier(CjTokens.MUT_KEYWORD)
    /**
     * 暴露 `isConst`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isConst: Boolean
        get() = hasModifier(CjTokens.CONST_KEYWORD)
}
