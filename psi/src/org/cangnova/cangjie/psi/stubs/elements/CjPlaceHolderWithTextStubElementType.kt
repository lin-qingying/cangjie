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

package org.cangnova.cangjie.psi.stubs.elements

import org.cangnova.cangjie.psi.CjElementImplStub
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub
import org.cangnova.cangjie.psi.stubs.impl.CangJiePlaceHolderWithTextStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

/**
 * 表示 `CjPlaceHolderWithTextStubElementType`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
class CjPlaceHolderWithTextStubElementType<T : CjElementImplStub<out StubElement<*>>>(debugName: String, psiClass: Class<T>) :
    CjStubElementType<CangJiePlaceHolderWithTextStub<T>, T>(debugName, psiClass, CangJiePlaceHolderWithTextStub::class.java) {

    /**
     * 实现 `createStub` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun createStub(psi: T, parentStub: StubElement<*>): CangJiePlaceHolderWithTextStub<T> {
        return CangJiePlaceHolderWithTextStubImpl(parentStub, this, psi.text)
    }

    /**
     * 实现 `serialize` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun serialize(stub: CangJiePlaceHolderWithTextStub<T>, dataStream: StubOutputStream) {
        dataStream.writeUTFFast(stub.text())
    }

    /**
     * 实现 `deserialize` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJiePlaceHolderWithTextStub<T> {
        val text = dataStream.readUTFFast()
        return CangJiePlaceHolderWithTextStubImpl(parentStub, this, text)
    }
}
