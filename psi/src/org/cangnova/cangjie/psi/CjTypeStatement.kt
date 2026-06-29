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
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.psiUtil.ClassIdCalculator
import org.cangnova.cangjie.psi.stubs.CangJieTypeStatementStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil


/**
 * 表示 `CjTypeStatement`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
abstract class CjTypeStatement :
    CjTypeParameterListOwnerStub<CangJieTypeStatementStub<out CjTypeStatement>>,
    CjDeclarationContainer,
    CjNamedDeclaration,
    CjPureTypeStatement,
    CjClassLikeDeclaration {

    companion object {
        val EMPTY_ARRAY: Array<CjTypeStatement?> = arrayOfNulls(0)
        private val declarationKeyword by lazy {

            TokenSet.create(
                CjTokens.CLASS_KEYWORD,
                CjTokens.INTERFACE_KEYWORD,
                CjTokens.ENUM_KEYWORD,
                CjTokens.STRUCT_KEYWORD,
                CjTokens.EXTEND_KEYWORD
            )

        }
    }

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieTypeStatementStub<out CjTypeStatement>, nodeType: IStubElementType<*, *>) : super(
        stub,
        nodeType,
    )

    /**
     * 暴露 `superTypeListEntries`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val superTypeListEntries: List<CjSuperTypeListEntry>
        get() = getSuperTypeList()?.entries.orEmpty()

    /**
     * 暴露 `declarations`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val declarations: List<CjDeclaration>
        get() = body?.declarations.orEmpty()

//    fun isTopLevel(): Boolean = stub?.isTopLevel() ?: (parent is CjFile)

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {

        return node.elementType.toString()
    }

    /**
     * 保存 `typeName`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    abstract val typeName: String


    /**
     * 保存 `declarationKeyword`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val declarationKeyword: PsiElement? get() = findChildByType(CjTypeStatement.declarationKeyword)

    /**
     * 保存 `variables`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val variables: List<CjFieldVariable> get() = body?.variables.orEmpty()
    /**
     * 保存 `properties`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val properties: List<CjProperty> get() = body?.properties.orEmpty()

    /**
     * 提供 `getSuperTypeList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getSuperTypeList(): CjSuperTypeList? = getStubOrPsiChild(CjStubElementTypes.SUPER_TYPE_LIST)

    /**
     * 提供 `addDeclaration` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    inline fun <reified T : CjDeclaration> addDeclaration(declaration: T): T {
        val body = getOrCreateBody()
        val anchor = PsiTreeUtil.skipSiblingsBackward(body.rBrace ?: body.lastChild!!, PsiWhiteSpace::class.java)
        return if (anchor?.nextSibling is PsiErrorElement) {
            body.addBefore(declaration, anchor)
        } else {
            body.addAfter(declaration, anchor)
        } as T
    }

    /**
     * 实现 `hasExplicitPrimaryConstructor` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasExplicitPrimaryConstructor(): Boolean = primaryConstructor != null

    /**
     * 提供 `hasSecondaryConstructors` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun hasSecondaryConstructors(): Boolean = !secondaryConstructors.isEmpty()

    /**
     * 实现 `hasPrimaryConstructor` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasPrimaryConstructor(): Boolean = hasExplicitPrimaryConstructor() || !hasSecondaryConstructors()

    /**
     * 暴露 `primaryConstructor`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val primaryConstructor: CjPrimaryConstructor?
        get() = body?.getStubOrPsiChild(CjStubElementTypes.PRIMARY_CONSTRUCTOR)

    /**
     * 暴露 `primaryConstructorModifierList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val primaryConstructorModifierList: CjModifierList?
        get() = primaryConstructor?.modifierList

    /**
     * 暴露 `primaryConstructorParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val primaryConstructorParameters: List<CjParameter>
        get() = getPrimaryConstructorParameterList()?.parameters.orEmpty()

    /**
     * 提供 `getPrimaryConstructorParameterList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getPrimaryConstructorParameterList(): CjParameterList? = primaryConstructor?.valueParameterList
    /**
     * 暴露 `secondaryConstructors`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val secondaryConstructors: List<CjSecondaryConstructor>
        get() = body?.secondaryConstructors.orEmpty()

    /**
     * 暴露 `primaryConstructors`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val primaryConstructors: List<CjPrimaryConstructor>
        get() = body?.primaryConstructors.orEmpty()

    /**
     * 暴露 `finalizers`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val finalizers: List<CjFinalizer>
        get() = body?.finalizers.orEmpty()
    /**
     * 保存 `constructors`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val constructors: List<CjConstructor<*>>
        get() =
            secondaryConstructors + primaryConstructors



    /**
     * 保存 `BODY_TYPE` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val BODY_TYPE = listOf(
        CjStubElementTypes.CLASS_BODY,
        CjStubElementTypes.INTERFACE_BODY,
        CjStubElementTypes.ENUM_BODY,
    )

    /**
     * 暴露 `body`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val body: CjAbstractClassBody?
        get() {
            for (type in BODY_TYPE) {
                val body = getStubOrPsiChild(type)
                if (body != null) {
                    return body // 找到匹配的子节点并返回
                }
            }
            return null // 如果没有找到匹配的类型，则返回 null
        }

    /**
     * 实现 `getClassId` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getClassId(): ClassId? {
        stub?.let { return it.getClassId() }
        return ClassIdCalculator.calculateClassId(this)
    }

    /**
     * 提供 `isExtend` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isExtend(): Boolean {
        return this is CjExtend
    }

    /**
     * 提供 `isStruct` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isStruct(): Boolean {
        return this is CjStruct
    }

    /**
     * 提供 `isInterface` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isInterface(): Boolean {
        return this is CjInterface
    }

    /**
     * 提供 `isSealed` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isSealed(): Boolean = hasModifier(CjTokens.SEALED_KEYWORD)

    /**
     * 提供 `isEnum` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isEnum(): Boolean {
        return this is CjEnum
    }


}

/**
 * 提供 `getOrCreateBody` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun CjTypeStatement.getOrCreateBody(): CjAbstractClassBody {
    body?.let { return it }

    val newBody = CjPsiFactory(project).createEmptyClassBody()
//    if (this is CjEnumConstructor) return addAfter(newBody, initializerList ?: nameIdentifier) as CjAbstractClassBody
    return add(newBody) as CjAbstractClassBody
}
