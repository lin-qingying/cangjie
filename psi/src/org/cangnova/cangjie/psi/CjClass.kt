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

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.psiUtil.ClassIdCalculator
import org.cangnova.cangjie.psi.stubs.CangJieClassStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.CLASS_BODY
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

/**
 * 表示 `CjClass`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
open class CjClass : CjTypeStatement {
//    fun isInterface(): Boolean =
//        _stub?.isInterface() ?: (findChildByType<PsiElement>(CjTokens.INTERFACE_KEYWORD) != null)

    /**
     * 保存 `_stub` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val _stub: CangJieClassStub?
        get() = stub as? CangJieClassStub

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieClassStub) : super(stub, CjStubElementTypes.CLASS)

    /**
     * 暴露 `typeName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeName: String
        get() = "class"
    /**
     * 暴露 `superTypeListEntries`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val superTypeListEntries: List<CjSuperTypeListEntry> get() = getSuperTypeList()?.entries.orEmpty()

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        return node.elementType.toString() + " : $name"
    }

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitClass(this, data)
    }

    /**
     * 暴露 `body`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val body: CjAbstractClassBody? get() = getStubOrPsiChild(CLASS_BODY)

    /**
     * 实现 `getClassId` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getClassId(): ClassId? {
        stub?.let { return it.getClassId() }
        return ClassIdCalculator.calculateClassId(this)
    }
}

/**
 * 提供 `createPrimaryConstructorIfAbsent` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun CjClass.createPrimaryConstructorIfAbsent(): CjPrimaryConstructor {
    val constructor = primaryConstructor
    if (constructor != null) return constructor
    var anchor: PsiElement? = typeParameterList
    if (anchor == null) anchor = nameIdentifier
    if (anchor == null) anchor = lastChild
    return addAfter(CjPsiFactory(project).createPrimaryConstructor(), anchor) as CjPrimaryConstructor
}

/**
 * 提供 `createPrimaryConstructorParameterListIfAbsent` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun CjClass.createPrimaryConstructorParameterListIfAbsent(): CjParameterList {
    val constructor = createPrimaryConstructorIfAbsent()
    val parameterList = constructor.valueParameterList
    if (parameterList != null) return parameterList
    return constructor.add(CjPsiFactory(project).createParameterList("()")) as CjParameterList
}
