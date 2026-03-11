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

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.psi.CjConstructorElementType
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.cangnova.cangjie.psi.stubs.CangJieConstructorStub
import org.cangnova.cangjie.psi.stubs.CangJieFinalizerStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieConstructorStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFinalizerStubImpl
import java.io.IOException
import org.jetbrains.annotations.NonNls

class CjFinalizerElementType(debugName: String) :
    CjStubElementType<CangJieFinalizerStub, CjFinalizer>(
        debugName,
        CjFinalizer::class.java,
        CangJieFinalizerStub::class.java,
    ) {
    override fun createPsi(stub: CangJieFinalizerStub): CjFinalizer {
        return CjFinalizer(stub)
    }

    override fun createPsiFromAst(node: ASTNode): CjFinalizer {
        return CjFinalizer(node)
    }

    override fun createStub(
        psi: CjFinalizer,
        parentStub: StubElement<*>,
    ): CangJieFinalizerStub {
        return CangJieFinalizerStubImpl(
            parentStub,
            CjStubElementTypes.FINALIZER,
            StringRef.fromString(psi.name),
            psi.hasBody(),
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieFinalizerStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.hasBody())
    }

    @Throws(IOException::class)
    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>,
    ): CangJieFinalizerStub {
        val name = dataStream.readName()
        val hasBody = dataStream.readBoolean()
        return CangJieFinalizerStubImpl(
            parentStub,
            CjStubElementTypes.FINALIZER,
            name,
            hasBody,
        )
    }

    override fun indexStub(stub: CangJieFinalizerStub, sink: IndexSink) {}
}

class CjSecondaryConstructorElementType(@NonNls debugName: String) :
    CjConstructorElementType<CjSecondaryConstructor>(
        debugName,
        CjSecondaryConstructor::class.java,
        CangJieConstructorStub::class.java,
    ) {
    override fun newStub(
        parentStub: StubElement<*>,
        nameRef: StringRef?,
        hasBody: Boolean,
        isPrimary: Boolean,
    ): CangJieConstructorStub<CjSecondaryConstructor> {
        return CangJieConstructorStubImpl(
            parentStub,
            CjStubElementTypes.SECONDARY_CONSTRUCTOR,
            nameRef,
            hasBody,
            isPrimary,
        )
    }
}
