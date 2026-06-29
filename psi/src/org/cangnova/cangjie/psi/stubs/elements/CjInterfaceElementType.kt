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

import org.cangnova.cangjie.psi.CjInterface
import org.cangnova.cangjie.psi.psiUtil.StubUtils.createClassId
import org.cangnova.cangjie.psi.psiUtil.StubUtils.deserializeClassId
import org.cangnova.cangjie.psi.psiUtil.StubUtils.serializeClassId
import org.cangnova.cangjie.psi.psiUtil.getSuperNames
import org.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import org.cangnova.cangjie.psi.stubs.CangJieInterfaceStub
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJieInterfaceStubImpl
import org.cangnova.cangjie.psi.stubs.impl.Utils.wrapStrings
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import java.io.IOException

/**
 * 表示 `CjInterfaceElementType`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
class CjInterfaceElementType(debugName: String) :
    CjStubElementType<CangJieInterfaceStub, CjInterface>(
        debugName,
        CjInterface::class.java,
        CangJieInterfaceStub::class.java,
    ) {
    /**
     * 实现 `indexStub` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun indexStub(stub: CangJieInterfaceStub, sink: IndexSink) {
        getInstance().indexInterface(stub, sink)
    }

    /**
     * 实现 `createPsi` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun createPsi(stub: CangJieInterfaceStub): CjInterface {
        return CjInterface(stub)
    }

    /**
     * 实现 `createPsiFromAst` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun createPsiFromAst(node: ASTNode): CjInterface {
        return CjInterface(node)
    }

    /**
     * 实现 `createStub` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun createStub(psi: CjInterface, parentStub: StubElement<out PsiElement?>): CangJieInterfaceStub {
        val fqName = psi.safeFqNameForLazyResolve()

        val superNames = psi.getSuperNames()
        val classId = createClassId(parentStub, psi)
        return CangJieInterfaceStubImpl(
            CjStubElementTypes.INTERFACE,
            parentStub,
            StringRef.fromString(fqName?.asString()),
            classId,
            StringRef.fromString(psi.name),
            wrapStrings(superNames),
        )
    }

    /**
     * 实现 `serialize` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    @Throws(IOException::class)
    override fun serialize(stub: CangJieInterfaceStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)

        val fqName = stub.getFqName()
        dataStream.writeName(fqName?.asString())

        serializeClassId(dataStream, stub.getClassId())

        val superNames = stub.getSuperNames()
        dataStream.writeVarInt(superNames.size)
        for (name in superNames) {
            dataStream.writeName(name)
        }
    }

    /**
     * 实现 `deserialize` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieInterfaceStub {
        val name = dataStream.readName()
        val qualifiedName = dataStream.readName()

        val classId = deserializeClassId(dataStream)

        val superCount = dataStream.readVarInt()
        val superNames = StringRef.createArray(superCount)
        for (i in 0..<superCount) {
            superNames[i] = dataStream.readName()
        }

        return CangJieInterfaceStubImpl(
            CjStubElementTypes.INTERFACE,
            parentStub,
            qualifiedName,
            classId,
            name,
            superNames,
        )
    }

    companion object {
        val stubType: CjInterfaceElementType
            get() = CjStubElementTypes.INTERFACE
    }
}
