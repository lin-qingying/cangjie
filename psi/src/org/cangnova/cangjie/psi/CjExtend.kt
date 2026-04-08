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
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.CangJieExtendStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

class CjExtend : CjTypeStatement {
    private val _stub: CangJieExtendStub?
        get() = stub as? CangJieExtendStub

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitExtend(this, data)
    }

    override val typeName: String
        get() = "extend"

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieExtendStub) : super(stub, CjStubElementTypes.EXTEND)

    override fun getName(): String? {
        _stub?.name?.let { return it }
        return getExtendName()
    }

    private fun getExtendName(): String? {
        return when (val type = receiverTypeReceiver?.typeElement) {
            is CjUserType -> type.referencedName
            is CjOptionType -> "Option"
            is CjBasicType -> type.name
            else -> null
        }
    }

    override fun getNameIdentifier(): PsiElement? {
        return when (val type = receiverTypeReceiver?.typeElement) {
            is CjUserType -> type.referenceExpression?.identifier
            is CjBasicType -> type
            is CjOptionType -> type
            else -> null
        }
    }

    /**
     * 被扩展的接收者类型。
     *
     * 对 stub-backed PSI，直接读取首个 `TYPE_REFERENCE` 子节点，避免无谓展开 AST。
     */
    val receiverTypeReceiver: CjTypeReference?
        get() {
            if (stub != null) {
                return getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE).firstOrNull()
            }
            return getReceiverTypeRefByTree()
        }

    override val nameAsSafeName: Name
        get() = receiverTypeReceiver?.text?.let { text ->
            if (text.isEmpty()) Name.ERROR_NAME else Name.identifier(text)
        } ?: Name.ERROR_NAME

    override val nameAsName: Name
        get() = name?.let(Name::identifier) ?: Name.ERROR_NAME

    /**
     * `extend` 的稳定身份。
     *
     * 优先使用 stub 中已经固化好的 ID；否则再根据当前 PSI 文本按统一算法构造。
     */
    fun getExtendId(): String {
        _stub?.extendId.takeIf { !it.isNullOrBlank() }?.let { return it }
        val packageFqName = (containingFile as? CjFile)?.packageFqName
        return buildExtendId(
            packageFqName = packageFqName,
            receiverTypeText = receiverTypeReceiver?.text,
            superTypeTexts = superTypeListEntries.mapNotNull { entry -> entry.typeReference?.text },
        )
    }

    private fun getReceiverTypeRefByTree(): CjTypeReference? {
        var child = firstChild
        while (child != null) {
            val tt = child.node.elementType
            if (tt === CjTokens.LPAR || tt === CjTokens.COLON) break
            if (child is CjTypeReference) {
                return child
            }
            child = child.nextSibling
        }

        return null
    }
}
