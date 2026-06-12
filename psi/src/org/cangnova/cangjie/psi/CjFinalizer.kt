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

class CjFinalizer : CjDeclarationStub<CangJieFinalizerStub>, CjFunction {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieFinalizerStub) : super(
        stub,
        CjStubElementTypes.FINALIZER,
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? = visitor.visitFinalizer(this, data)

    fun getContainingTypeStatement() = parent?.parent as CjTypeStatement

    val initKeyword: PsiElement?
        get() = findChildByType(CjTokens.INIT_KEYWORD)

    override val isLocal = false

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

    override val valueParameters: List<CjParameter>
        get() = valueParameterList?.parameters ?: emptyList()

    override val typeReference: CjTypeReference? = null

    override val valueParameterList: CjParameterList?
        get() = getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST)

    override fun setTypeReference(typeRef: CjTypeReference?) =
        throw IncorrectOperationException("setTypeReference to finalizer")

    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)

    override val equalsToken = null

    override fun hasBlockBody() = hasBody()

    override fun hasBody(): Boolean {
        stub?.let { return it.hasBody() }
        return bodyExpression != null
    }

    override val typeParameterList: CjTypeParameterList?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_PARAMETER_LIST)

    override val typeConstraintList: CjTypeConstraintList?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_CONSTRAINT_LIST)

    override val typeConstraints: List<CjTypeConstraint>
        get() = typeConstraintList?.constraints ?: emptyList()

    override val typeParameters: List<CjTypeParameter>
        get() = typeParameterList?.parameters ?: emptyList()

    override fun hasDeclaredReturnType() = false

    override fun getName(): String? = getContainingTypeStatement().name

    override val fqName: FqName?
        get() = null

    override val nameAsSafeName: Name
        get() = CjPsiUtil.safeName(name)

    override val nameAsName: Name?
        get() = nameAsSafeName

    override fun getNameIdentifier() = null

    override fun getIdentifyingElement(): PsiElement? = initKeyword

    override fun setName(name: String): PsiElement =
        throw IncorrectOperationException("setName to finalizer")

    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)

    fun hasInitKeyword(): Boolean = stub != null || initKeyword != null

    override fun getTextOffset(): Int {
        return initKeyword?.textOffset
            ?: valueParameterList?.textOffset
            ?: super.getTextOffset()
    }

    override fun getUseScope(): SearchScope {
        return getContainingTypeStatement().useScope
    }
}
