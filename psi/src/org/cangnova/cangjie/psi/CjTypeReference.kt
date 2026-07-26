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
import org.cangnova.cangjie.psi.psiUtil.CjStubbedPsiUtil
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.CjTokenSets
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

/**
 *键入Reference Element。
 *底层令牌为[org.cangnova.cangjie.psi.CjNodeTypes.TYPE_REFERENCE]
 */
class CjTypeReference :
    CjModifierListOwnerStub<CangJiePlaceHolderStub<CjTypeReference>>,
    CjElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjTypeReference>) : super(stub, CjStubElementTypes.TYPE_REFERENCE)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitTypeReference(this, data)
    }


    /**
     * 保存 `isPlaceholder`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isPlaceholder: Boolean
        get() = ((typeElement as? CjUserType)?.referenceExpression as? CjNameReferenceExpression)?.isPlaceholder == true

    /**
     * 保存 `typeElement`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeElement: CjTypeElement?
        get() {
            return CjStubbedPsiUtil.getStubOrPsiChild(this, CjTokenSets.TYPE_ELEMENT_TYPES, CjTypeElement.ARRAY_FACTORY)

             /*   ?: if (children.isNotEmpty() && children[0].elementType == CjNodeTypes.BASIC_TYPE) {
                    children[0] as CjTypeElement
                } else {
                    null
                }*/
        }

    /**
     * 提供 `hasParentheses` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun hasParentheses(): Boolean {
        return findChildByType<PsiElement>(CjTokens.LPAR) != null && findChildByType<PsiElement>(CjTokens.RPAR) != null
    }

    /**
     * 提供 `nameForReceiverLabel` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun nameForReceiverLabel() = (typeElement as? CjUserType)?.referencedName

    /**
     * 提供 `getTypeText` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getTypeText(): String {
        return stub?.let { getTypeText(typeElement) } ?: text
    }

    /**
     * 执行 `getQualifiedName` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     *
     * 类型位置不要对 Unit/Int64 等类型关键字加反引号；`` 只用于标识符位置转义关键字。
     */
    private fun getQualifiedName(userType: CjUserType): String? {
        val referencedName = userType.referencedName ?: return null
        val qualifier = userType.qualifier ?: return referencedName
        return getQualifiedName(qualifier) + "." + referencedName
    }

    /**
     * 执行 `getTypeText` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun getTypeText(typeElement: CjTypeElement?): String? {
        return when (typeElement) {
            is CjUserType -> buildString {
                append(getQualifiedName(typeElement))
                val args = typeElement.typeArguments
                if (args.isNotEmpty()) {
                    append(args.joinToString(", ", "<", ">") {
                        val projection = when (it.projectionKind) {
                            CjProjectionKind.NONE -> ""
                        }
                        projection + (it.typeReference?.getTypeText() ?: "")
                    })
                }
            }

            is CjBasicType -> buildString {
                append(typeElement.name)
            }

            is CjFunctionType -> buildString {
                typeElement.receiverTypeReference?.let { append(getTypeText(it.typeElement)) }
                append(
                    typeElement.parameters.joinToString(", ", "(", ")") { param ->
                        param.name?.let { "$it: " }.orEmpty() + param.typeReference?.getTypeText().orEmpty()
                    },
                )
                typeElement.returnTypeReference?.let { returnType ->
                    append(" -> ")
                    append(getTypeText(returnType.typeElement))
                }
            }

            is CjTupleType -> typeElement.typeArgumentsAsTypes.joinToString(", ", "(", ")") { typeReference ->
                typeReference.getTypeText()
            }

            is CjParenthesizedType -> typeElement.typeArgumentsAsTypes.singleOrNull()?.getTypeText()?.let { "($it)" }

            is CjOptionType -> typeElement.getInnerType()?.let { "?${getTypeText(it)}" }

            is CjVArrayType -> buildString {
                append("VArray<")
                append(typeElement.typeReference?.getTypeText().orEmpty())
                typeElement.literal?.text?.takeIf(String::isNotBlank)?.let { literal ->
                    append(", \$")
                    append(literal)
                }
                append(">")
            }

            null -> null
            else -> error("Unsupported type $typeElement")
        }
    }
}
