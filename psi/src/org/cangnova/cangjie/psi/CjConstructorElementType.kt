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

import org.cangnova.cangjie.psi.stubs.CangJieConstructorStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import java.io.IOException

/**
 * 表示 `CjConstructorElementType`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
abstract class CjConstructorElementType<T : CjConstructor<T>>(
    debugName: String,
    tClass: Class<T>,
    stubClass: Class<CangJieConstructorStub<*>>,
) : CjStubElementType<CangJieConstructorStub<T>, T>(debugName, tClass, stubClass) {
    /**
     * 提供 `newStub` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    protected abstract fun newStub(
        parentStub: StubElement<*>,
        nameRef: StringRef?,
        hasBody: Boolean,
        isPrimary: Boolean,
    ): CangJieConstructorStub<T>

    /**
     * 保存 `isPrimary`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    open val isPrimary: Boolean get() = false

    /**
     * 实现 `createStub` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun createStub(psi: T, parentStub: StubElement<*>): CangJieConstructorStub<T> {
        val hasBody = psi.hasBody()
        return newStub(
            parentStub,
            StringRef.fromString(psi.name),
            hasBody,
            isPrimary,
        )
    }

    /**
     * 实现 `serialize` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    @Throws(IOException::class)
    override fun serialize(stub: CangJieConstructorStub<T>, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.hasBody())
        dataStream.writeBoolean(stub.isPrimary)
    }

    /**
     * 实现 `deserialize` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieConstructorStub<T> {
        val name = dataStream.readName()
        val hasBody = dataStream.readBoolean()
        val isPrimary = dataStream.readBoolean()
        return newStub(parentStub, name, hasBody, isPrimary)
    }

    /**
     * 实现 `indexStub` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun indexStub(stub: CangJieConstructorStub<T>, sink: IndexSink) {
    }
}
