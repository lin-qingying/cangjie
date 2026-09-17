/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.psi.stubs.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.cangnova.cangjie.psi.CjFeaturesDirective
import org.cangnova.cangjie.psi.stubs.CangJieFeaturesDirectiveStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieFeaturesDirectiveStubImpl
import java.io.IOException

/** 文件级 `features` directive 的 stub element type。 */
class CjFeaturesDirectiveElementType(debugName: String) :
    CjStubElementType<CangJieFeaturesDirectiveStub, CjFeaturesDirective>(
        debugName,
        CjFeaturesDirective::class.java,
        CangJieFeaturesDirectiveStub::class.java,
    ) {
    override fun createStub(
        psi: CjFeaturesDirective,
        parentStub: StubElement<out PsiElement?>,
    ): CangJieFeaturesDirectiveStub =
        CangJieFeaturesDirectiveStubImpl(parentStub, psi.featureIds)

    @Throws(IOException::class)
    override fun serialize(stub: CangJieFeaturesDirectiveStub, dataStream: StubOutputStream) {
        dataStream.writeInt(stub.featureIds.size)
        stub.featureIds.forEach(dataStream::writeUTF)
    }

    @Throws(IOException::class)
    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>,
    ): CangJieFeaturesDirectiveStub {
        val count = dataStream.readInt()
        require(count >= 0) { "Invalid features directive stub size: $count" }
        return CangJieFeaturesDirectiveStubImpl(
            parent = parentStub,
            featureIds = List(count) { dataStream.readUTF() },
        )
    }
}
