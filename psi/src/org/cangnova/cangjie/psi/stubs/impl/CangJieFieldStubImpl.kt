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
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.stubs.CangJieFieldStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 类成员字段的 Stub 实现
 *
 * @param parent 父 Stub 元素
 * @param name 字段名称
 * @param fqName 完全限定名
 * @param isVar 是否为 var 声明
 * @param isConst 是否为 const 声明
 * @param hasInitializer 是否有初始化器
 * @param hasReturnTypeRef 是否有类型声明
 * @param origin Stub 来源信息
 */
class CangJieFieldStubImpl(
    parent: StubElement<out PsiElement>?,
    /**
     * 保存 `name` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val name: StringRef?,
    /**
     * 保存 `fqName` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val fqName: FqName?,
    /**
     * 保存 `isVar` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val isVar: Boolean,
    /**
     * 保存 `isConst` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val isConst: Boolean,
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
) : CangJieStubBaseImpl<CjFieldVariable>(parent, CjStubElementTypes.FIELD), CangJieFieldStub {

    /**
     * 实现 `getFqName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getFqName(): FqName? = fqName
    /**
     * 实现 `isVar` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isVar(): Boolean = isVar
    /**
     * 实现 `isConst` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isConst(): Boolean = isConst
    /**
     * 实现 `hasInitializer` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasInitializer(): Boolean = hasInitializer
    /**
     * 实现 `hasReturnTypeRef` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasReturnTypeRef(): Boolean = hasReturnTypeRef
    /**
     * 实现 `getName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String? = StringRef.toString(name)

    /**
     * 实现 `copyInto` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun copyInto(newParent: StubElement<*>?): CangJieFieldStubImpl = CangJieFieldStubImpl(
        parent = newParent,
        name = name,
        fqName = fqName,
        isVar = isVar,
        isConst = isConst,
        hasInitializer = hasInitializer,
        hasReturnTypeRef = hasReturnTypeRef,
        origin = origin,
    )
}
