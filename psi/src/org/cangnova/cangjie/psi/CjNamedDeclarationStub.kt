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

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.psiUtil.astReplace
import org.cangnova.cangjie.psi.psiUtil.quoteIfNeeded
import org.cangnova.cangjie.psi.stubs.CangJieStubWithFqName
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorName
import com.intellij.lang.ASTNode
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.util.IncorrectOperationException

/**
 * 表示 `CjNamedDeclarationStub`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
abstract class CjNamedDeclarationStub<T : CangJieStubWithFqName<*>> : CjDeclarationStub<T>, CjNamedDeclaration {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    constructor(node: ASTNode) : super(node)

    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String? {
        return runReadAction {
            val stub = stub
            if (stub != null) {
                return@runReadAction stub.name
            }

            val identifier = nameIdentifier
            if (identifier != null) {
                val text = identifier.text

                return@runReadAction if (text != null) CjPsiUtil.unquoteIdentifier(text) else null
            }

            return@runReadAction null
        }
    }

    /**
     * 暴露 `nameAsName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val nameAsName: Name?
        get() {
            return this.name?.asOperatorName()
        }

    /**
     * 暴露 `nameAsSafeName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val nameAsSafeName: Name
        get() = CjPsiUtil.safeName(name)

    /**
     * 实现 `getNameIdentifier` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getNameIdentifier(): PsiElement? {
        return findChildByType(CjTokens.IDENTIFIER) ?: findChildByType(CjNodeTypes.OPERATION_NAME)
    }

    /**
     * 保存 `operatorName`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val operatorName: CjOperationName?
        get() = findChildByType(CjNodeTypes.OPERATION_NAME)

    /**
     * 实现 `setName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement? {
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
     * 实现 `getTextOffset` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getTextOffset(): Int {
        val identifier = nameIdentifier
        return identifier?.textRange?.startOffset ?: textRange.startOffset
    }

    /**
     * 实现 `getUseScope` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getUseScope(): SearchScope {
        return computeCangJieDeclarationUseScope(
            declaration = this,
            defaultScope = super.getUseScope(),
        )
    }

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
}
