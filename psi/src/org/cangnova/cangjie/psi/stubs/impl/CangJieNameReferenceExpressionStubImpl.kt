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

import org.cangnova.cangjie.psi.CjNameBasicReferenceExpression
import org.cangnova.cangjie.psi.CjNameReferenceExpression
import org.cangnova.cangjie.psi.stubs.CangJieNameBasicReferenceExpressionStub
import org.cangnova.cangjie.psi.stubs.CangJieNameReferenceExpressionStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef

/**
 * 表示 `CangJieNameReferenceExpressionStubImpl`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
class CangJieNameReferenceExpressionStubImpl :
    CangJieStubBaseImpl<CjNameReferenceExpression>,
    CangJieNameReferenceExpressionStub {
    /**
     * 保存 `referencedName` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val referencedName: StringRef
    /**
     * 保存 `isClassRef`，供PSI Stub流程读取节点结构或语义信息。
     */
    val isClassRef: Boolean

    constructor(parent: StubElement<*>?, referencedName: StringRef) : super(
        parent,
        CjStubElementTypes.REFERENCE_EXPRESSION,
    ) {
        this.referencedName = referencedName
        isClassRef = false
    }

    constructor(
        parent: StubElement<*>?,
        referencedName: StringRef,
        myClassRef: Boolean,
    ) : super(parent, CjStubElementTypes.REFERENCE_EXPRESSION) {
        this.referencedName = referencedName
        this.isClassRef = myClassRef
    }

    /**
     * 实现 `getReferencedName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getReferencedName(): String {
        return referencedName.string
    }

    /**
     * 实现 `copyInto` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun copyInto(newParent: StubElement<*>?): CangJieNameReferenceExpressionStubImpl = CangJieNameReferenceExpressionStubImpl(
        parent = newParent,
        referencedName = referencedName,
        myClassRef = isClassRef,
    )
}
/**
 * 表示 `CangJieNameBasicReferenceExpressionStubImpl`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
class CangJieNameBasicReferenceExpressionStubImpl :
    CangJieStubBaseImpl<CjNameBasicReferenceExpression>,
    CangJieNameBasicReferenceExpressionStub {
    /**
     * 保存 `referencedName` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val referencedName: StringRef
    /**
     * 保存 `isClassRef`，供PSI Stub流程读取节点结构或语义信息。
     */
    val isClassRef: Boolean

    constructor(parent: StubElement<*>?, referencedName: StringRef) : super(
        parent,
        CjStubElementTypes.BASIC_REFERENCE_EXPRESSION,
    ) {
        this.referencedName = referencedName
        isClassRef = false
    }

    constructor(
        parent: StubElement<*>?,
        referencedName: StringRef,
        myClassRef: Boolean,
    ) : super(parent, CjStubElementTypes.BASIC_REFERENCE_EXPRESSION) {
        this.referencedName = referencedName
        this.isClassRef = myClassRef
    }

    /**
     * 实现 `getReferencedName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getReferencedName(): String {
        return referencedName.string
    }

    /**
     * 实现 `copyInto` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun copyInto(newParent: StubElement<*>?): CangJieNameBasicReferenceExpressionStubImpl = CangJieNameBasicReferenceExpressionStubImpl(
        parent = newParent,
        referencedName = referencedName,
        myClassRef = isClassRef,
    )
}
