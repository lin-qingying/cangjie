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

import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.addModifier
import org.cangnova.cangjie.psi.stubs.CangJieConstructorStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

// 主构造函数
/**
 * 表示 `CjPrimaryConstructor`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjPrimaryConstructor : CjConstructor<CjPrimaryConstructor> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieConstructorStub<CjPrimaryConstructor>) : super(stub, CjStubElementTypes.PRIMARY_CONSTRUCTOR)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? = visitor.visitPrimaryConstructor(this, data)

    /**
     * 实现 `getContainingTypeStatement` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getContainingTypeStatement() = parent?.parent as CjTypeStatement

    /**
     * 执行 `getOrCreateConstructorKeyword` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun getOrCreateConstructorKeyword(): PsiElement {
        return getInitKeyword() ?: addBefore(CjPsiFactory(project).createConstructorKeyword(), valueParameterList!!)
    }

    /**
     * 执行 `removeRedundantConstructorKeywordAndSpace` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun removeRedundantConstructorKeywordAndSpace() {
        getInitKeyword()?.delete()
        if (prevSibling is PsiWhiteSpace) {
            prevSibling.delete()
        }
    }

    /**
     * 实现 `getInitKeyword` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getInitKeyword(): PsiElement? {
        return identifier
    }
    /**
     * 实现 `addModifier` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun addModifier(modifier: CjKeywordToken) {
        val modifierList = modifierList
        if (modifierList != null) {
            addModifier(modifierList, modifier)
            if (this.modifierList == null) {
                getInitKeyword()?.delete()
            }
        } else {
            if (modifier == CjTokens.PUBLIC_KEYWORD) return
            val newModifierList = CjPsiFactory(project).createModifierList(modifier)
            addBefore(newModifierList, getOrCreateConstructorKeyword())
        }
    }

    /**
     * 实现 `getIdentifyingElement` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getIdentifyingElement(): PsiElement? {
        return identifier
    }

    /**
     * 保存 `identifier`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val identifier: PsiElement?
        get() {
            // 优先从 Stub 获取，避免访问 AST
            val stubIdentifier = greenStub?.name
            if (stubIdentifier != null) {
                // Stub 中有标识符名称，但我们需要返回 PsiElement
                // 如果只是为了获取名称，不需要 PsiElement，直接用 getName()
                // 这里仍然需要找到实际的 PsiElement
                if (containingFile.isValid) {
                    return findChildByType(CjTokens.IDENTIFIER)
                }
                return null
            }
            return findChildByType(CjTokens.IDENTIFIER)
        }

    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String? {
        // 优先从 Stub 获取，避免访问 AST 导致 PsiInvalidElementAccessException
        val stub = greenStub
        if (stub != null) {
            val identifierName = stub.name
            if (identifierName != null) {
                return identifierName
            }
        }
        // 如果 Stub 不可用，再访问 AST
        return try {
            if (containingFile.isValid) {
                identifier?.text
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    /**
     * 实现 `removeModifier` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun removeModifier(modifier: CjKeywordToken) {
        super.removeModifier(modifier)
        if (modifierList == null) {
            removeRedundantConstructorKeywordAndSpace()
        }
    }
    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        return node.elementType.toString()
    }
}
