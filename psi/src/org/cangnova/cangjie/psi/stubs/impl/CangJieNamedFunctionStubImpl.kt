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

import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjMainFunction
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.stubs.CangJieNamedFunctionStub
import org.cangnova.cangjie.psi.stubs.CangJieMainFunctionStub
import org.cangnova.cangjie.psi.stubs.CangJieMacroStub
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.name.*

/**
 * 表示 `CangJieNamedFunctionStubImpl`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
class CangJieNamedFunctionStubImpl(
    parent: StubElement<out PsiElement>?,
    element: IStubElementType<*, *>,
    /**
     * 保存 `nameRef` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val nameRef: StringRef?,
    /**
     * 保存 `isTopLevel` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val isTopLevel: Boolean,
    /**
     * 保存 `fqName` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val fqName: FqName?,
    /**
     * 保存 `hasBlockBody` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val hasBlockBody: Boolean,
    /**
     * 保存 `hasBody` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val hasBody: Boolean,
    /**
     * 保存 `hasTypeParameterListBeforeFunctionName` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val hasTypeParameterListBeforeFunctionName: Boolean,
    /**
     * 保存 `origin`，供PSI Stub流程读取节点结构或语义信息。
     */
    val origin: CangJieStubOrigin?,
) : CangJieStubBaseImpl<CjNamedFunction>(parent, element), CangJieNamedFunctionStub {
    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level functions")
        }
    }

    /**
     * 实现 `getFqName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getFqName() = fqName

    /**
     * 实现 `getName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName() = StringRef.toString(nameRef)
    /**
     * 实现 `isTopLevel` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isTopLevel() = isTopLevel
    /**
     * 实现 `hasBlockBody` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBlockBody() = hasBlockBody
    /**
     * 实现 `hasBody` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBody() = hasBody
    /**
     * 实现 `hasTypeParameterListBeforeFunctionName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasTypeParameterListBeforeFunctionName() = hasTypeParameterListBeforeFunctionName

    /**
     * 实现 `copyInto` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun copyInto(newParent: StubElement<*>?): CangJieNamedFunctionStubImpl = CangJieNamedFunctionStubImpl(
        parent = newParent,
        element = stubType,
        nameRef = nameRef,
        isTopLevel = isTopLevel,
        fqName = fqName,
        hasBlockBody = hasBlockBody,
        hasBody = hasBody,
        hasTypeParameterListBeforeFunctionName = hasTypeParameterListBeforeFunctionName,
        origin = origin,
    )

    companion object
}

/**
 * 表示 `CangJieMainFunctionStubImpl`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
class CangJieMainFunctionStubImpl(
    parent: StubElement<out PsiElement>?,
    element: IStubElementType<*, *>,
    /**
     * 保存 `nameRef` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val nameRef: StringRef?,

    /**
     * 保存 `fqName` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val fqName: FqName?,

    /**
     * 保存 `origin`，供PSI Stub流程读取节点结构或语义信息。
     */
    val origin: CangJieStubOrigin?,
) : CangJieStubBaseImpl<CjMainFunction>(parent, element), CangJieMainFunctionStub {


    /**
     * 实现 `getFqName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getFqName() = fqName

    /**
     * 实现 `getName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName() = StringRef.toString(nameRef)
    /**
     * 实现 `isTopLevel` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isTopLevel() = true

    /**
     * 实现 `copyInto` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun copyInto(newParent: StubElement<*>?): CangJieMainFunctionStubImpl = CangJieMainFunctionStubImpl(
        parent = newParent,
        element = stubType,
        nameRef = nameRef,
        fqName = fqName,
        origin = origin,
    )
}

/**
 * 表示 `CangJieMacroStubImpl`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
class CangJieMacroStubImpl(
    parent: StubElement<out PsiElement>?,
    element: IStubElementType<*, *>,
    /**
     * 保存 `nameRef` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val nameRef: StringRef?,
    /**
     * 保存 `isTopLevel` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val isTopLevel: Boolean,
    /**
     * 保存 `fqName` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val fqName: FqName?,
    /**
     * 保存 `hasBlockBody` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val hasBlockBody: Boolean,
    /**
     * 保存 `hasBody` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val hasBody: Boolean,
    /**
     * 保存 `hasTypeParameterListBeforeFunctionName` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val hasTypeParameterListBeforeFunctionName: Boolean,
    /**
     * 保存 `origin`，供PSI Stub流程读取节点结构或语义信息。
     */
    val origin: CangJieStubOrigin?,
) : CangJieStubBaseImpl<CjMacroDeclaration>(parent, element), CangJieMacroStub {
    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level functions")
        }
    }

    /**
     * 实现 `getFqName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getFqName() = fqName

    /**
     * 实现 `getName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName() = StringRef.toString(nameRef)
    /**
     * 实现 `isTopLevel` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isTopLevel() = isTopLevel
    /**
     * 实现 `hasBlockBody` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBlockBody() = hasBlockBody
    /**
     * 实现 `hasBody` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBody() = hasBody
    /**
     * 实现 `hasTypeParameterListBeforeFunctionName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasTypeParameterListBeforeFunctionName() = hasTypeParameterListBeforeFunctionName

    /**
     * 实现 `copyInto` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun copyInto(newParent: StubElement<*>?): CangJieMacroStubImpl = CangJieMacroStubImpl(
        parent = newParent,
        element = stubType,
        nameRef = nameRef,
        isTopLevel = isTopLevel,
        fqName = fqName,
        hasBlockBody = hasBlockBody,
        hasBody = hasBody,
        hasTypeParameterListBeforeFunctionName = hasTypeParameterListBeforeFunctionName,
        origin = origin,
    )
}
