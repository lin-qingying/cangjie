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

import org.cangnova.cangjie.psi.CjModifierList
import org.cangnova.cangjie.psi.stubs.CangJieCompiledDeclarationMetadata
import org.cangnova.cangjie.psi.stubs.CangJieCompiledSetterParameter
import org.cangnova.cangjie.psi.stubs.CangJieCompiledDeclarationStatus
import org.cangnova.cangjie.psi.stubs.CangJieCompiledDeclarationStatus.Flag
import org.cangnova.cangjie.psi.stubs.CangJieModifierListStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieModifierListStubImpl
import org.cangnova.cangjie.psi.stubs.impl.ModifierMaskUtils.computeMaskFromModifierList
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.DataInputOutputUtil
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import java.io.IOException

/**
 * 表示 `CjModifierListElementType`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
class CjModifierListElementType<T : CjModifierList>(debugName: String, psiClass: Class<T>) :
    CjStubElementType<CangJieModifierListStub, T>(debugName, psiClass, CangJieModifierListStub::class.java) {
    /**
     * 实现 `createStub` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun createStub(psi: T, parentStub: StubElement<*>?): CangJieModifierListStub {
        return CangJieModifierListStubImpl(parentStub, computeMaskFromModifierList(psi), this)
    }

    /**
     * 实现 `serialize` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    @Throws(IOException::class)
    override fun serialize(stub: CangJieModifierListStub, dataStream: StubOutputStream) {
        val mask = (stub as CangJieModifierListStubImpl).mask
        DataInputOutputUtil.writeLONG(dataStream, mask)
        val metadata = stub.compiledDeclarationMetadata
        writeCompiledStatus(metadata?.status, dataStream)
        if (metadata != null) {
            writeCompiledStatus(metadata.getterStatus, dataStream)
            writeCompiledStatus(metadata.setterStatus, dataStream)
            dataStream.writeBoolean(metadata.declarationIndex != null)
            metadata.declarationIndex?.let { DataInputOutputUtil.writeINT(dataStream, it) }
            dataStream.writeBoolean(metadata.setterParameter != null)
            metadata.setterParameter?.let { parameter ->
                dataStream.writeName(parameter.name)
                dataStream.writeBoolean(parameter.isNameSpecial)
                dataStream.writeBoolean(parameter.isNamed)
                dataStream.writeBoolean(parameter.hasDefaultValue)
                writeCompiledStatus(parameter.status, dataStream)
            }
        }
    }

    /**
     * 实现 `deserialize` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieModifierListStub {
        val mask = DataInputOutputUtil.readLONG(dataStream)
        val status = readCompiledStatus(dataStream)
        val metadata = status?.let {
            val getter = readCompiledStatus(dataStream)
            val setter = readCompiledStatus(dataStream)
            val index = if (dataStream.readBoolean()) DataInputOutputUtil.readINT(dataStream).also { value ->
                if (value < 0) throw IOException("Negative compiled declaration index: $value")
            } else null
            val parameter = if (dataStream.readBoolean()) {
                val name = dataStream.readNameString()?.takeIf(String::isNotBlank)
                    ?: throw IOException("Missing compiled setter parameter name")
                val special = dataStream.readBoolean()
                val named = dataStream.readBoolean()
                val defaultValue = dataStream.readBoolean()
                val parameterStatus = readCompiledStatus(dataStream)
                    ?: throw IOException("Missing compiled setter parameter status")
                CangJieCompiledSetterParameter(name, special, named, defaultValue, parameterStatus)
            } else null
            CangJieCompiledDeclarationMetadata(it, getter, setter, index, parameter)
        }
        return CangJieModifierListStubImpl(parentStub, mask, this, metadata)
    }
}

/** 状态段只编码 metadata 已确认的值，不能与语法 modifier mask 合并。 */
private fun writeCompiledStatus(status: CangJieCompiledDeclarationStatus?, stream: StubOutputStream) {
    stream.writeBoolean(status != null)
    if (status == null) return

    stream.writeName(status.visibility.name)
    stream.writeByte(status.modality.ordinal)
    stream.writeByte(status.visibilityExplicitness.ordinal)
    stream.writeByte(status.modalityExplicitness.ordinal)
    stream.writeByte(status.bodyPresence.ordinal)
    DataInputOutputUtil.writeLONG(stream, status.flags.fold(0L) { mask, flag -> mask or (1L shl flag.ordinal) })
}

private fun readCompiledStatus(stream: StubInputStream): CangJieCompiledDeclarationStatus? {
    if (!stream.readBoolean()) return null

    val visibility = when (val name = stream.readNameString()) {
        Visibilities.Public.name -> Visibilities.Public
        Visibilities.Private.name -> Visibilities.Private
        Visibilities.Protected.name -> Visibilities.Protected
        Visibilities.Internal.name -> Visibilities.Internal
        Visibilities.Local.name -> Visibilities.Local
        Visibilities.PrivateToThis.name -> Visibilities.PrivateToThis
        Visibilities.Inherited.name -> Visibilities.Inherited
        Visibilities.InvisibleFake.name -> Visibilities.InvisibleFake
        Visibilities.Unknown.name -> Visibilities.Unknown
        else -> throw IOException("Unknown compiled declaration visibility: $name")
    }
    val modality = readCompiledStatusEntry(stream, Modality.entries, "modality")
    val visibilityExplicitness = readCompiledStatusEntry(
        stream, CangJieCompiledDeclarationStatus.Explicitness.entries, "visibility explicitness",
    )
    val modalityExplicitness = readCompiledStatusEntry(
        stream, CangJieCompiledDeclarationStatus.Explicitness.entries, "modality explicitness",
    )
    val bodyPresence = readCompiledStatusEntry(
        stream, CangJieCompiledDeclarationStatus.BodyPresence.entries, "body presence",
    )
    val mask = DataInputOutputUtil.readLONG(stream)
    val knownMask = Flag.entries.fold(0L) { result, flag -> result or (1L shl flag.ordinal) }
    if (mask and knownMask.inv() != 0L) throw IOException("Unknown compiled declaration status flags: $mask")
    val flags = buildSet { Flag.entries.filterTo(this) { mask and (1L shl it.ordinal) != 0L } }
    return CangJieCompiledDeclarationStatus(
        visibility, modality, visibilityExplicitness, modalityExplicitness, bodyPresence, flags,
    )
}

/** 非法 ordinal 与未知 visibility/flag 一样属于存根协议错误，不泄漏数组下标异常或造默认状态。 */
private fun <T> readCompiledStatusEntry(stream: StubInputStream, entries: List<T>, fieldName: String): T {
    val ordinal = stream.readUnsignedByte()
    if (ordinal !in entries.indices) throw IOException("Unknown compiled declaration $fieldName ordinal: $ordinal")
    return entries[ordinal]
}
