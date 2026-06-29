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

package org.cangnova.cangjie.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.stubs.CangJieVariableStub
import org.cangnova.cangjie.psi.stubs.PatternKind
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 变量声明 Stub 实现
 *
 * 变量声明是模式匹配的声明方式，支持绑定模式、元组模式、枚举模式、通配符模式。
 * 变量本身没有名称，名称信息来自模式匹配中的绑定模式子节点。
 * fqName 存储在子模式（CangJieBindingPatternStub）中，而不是变量本身。
 *
 * @param parent 父 Stub 元素
 * @param patternKind 模式类型
 * @param isVar 是否为 var 声明
 * @param isTopLevel 是否为顶层变量
 * @param hasInitializer 是否有初始化器
 * @param hasReturnTypeRef 是否有类型声明
 * @param origin Stub 来源信息
 */
class CangJieVariableStubImpl(
    parent: StubElement<out PsiElement>?,
    /**
     * 保存 `patternKind` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val patternKind: PatternKind,
    /**
     * 保存 `isVar` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val isVar: Boolean,
    /**
     * 保存 `isConst` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val isConst: Boolean,
    /**
     * 保存 `isTopLevel` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val isTopLevel: Boolean,
    /**
     * 保存 `hasInitializer` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val hasInitializer: Boolean,
    /**
     * 保存 `hasReturnTypeRef` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val hasReturnTypeRef: Boolean,
    /**
     * 保存 `origin`，供PSI Stub流程读取节点结构或语义信息。
     */
    val origin: CangJieStubOrigin?,
) : CangJieStubBaseImpl<CjPatternVariable>(parent, CjStubElementTypes.VARIABLE), CangJieVariableStub {

    /**
     * 实现 `getPatternKind` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getPatternKind(): PatternKind = patternKind
    /**
     * 实现 `isVar` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isVar(): Boolean = isVar
    /**
     * 实现 `isConst` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isConst(): Boolean = isConst
    /**
     * 实现 `isTopLevel` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isTopLevel(): Boolean = isTopLevel
    /**
     * 实现 `hasInitializer` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasInitializer(): Boolean = hasInitializer
    /**
     * 实现 `hasReturnTypeRef` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasReturnTypeRef(): Boolean = hasReturnTypeRef

    /**
     * 实现 `copyInto` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun copyInto(newParent: StubElement<*>?): CangJieVariableStubImpl = CangJieVariableStubImpl(
        parent = newParent,
        patternKind = patternKind,
        isVar = isVar,
        isConst = isConst,
        isTopLevel = isTopLevel,
        hasInitializer = hasInitializer,
        hasReturnTypeRef = hasReturnTypeRef,
        origin = origin,
    )
}
