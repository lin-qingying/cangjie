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
import com.intellij.psi.stubs.ObjectStubSerializer
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.util.io.AbstractStringEnumerator
import com.intellij.util.io.UnsyncByteArrayOutputStream
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import org.cangnova.cangjie.psi.stubs.elements.CjFileElementType
import org.cangnova.cangjie.utils.checkWithAttachment

/**
 * 仓颉文件 Stub 的实现类。
 *
 * 通过 [CangJieFileStubKind] 来区分不同类型的文件 Stub。
 *
 * @param file 关联的 PSI 文件，可以为 null（在索引构建期间）
 * @param kind 文件 Stub 的类型
 */
class CangJieFileStubImpl(
    file: CjFile?,
    override val kind: CangJieFileStubKind,
) : PsiFileStubImpl<CjFile>(file), CangJieFileStub {

    /**
     * 兼容旧构造函数：仅指定包名的普通文件。
     */
    constructor(file: CjFile?, packageName: String) : this(
        file = file,
        kind = CangJieFileStubKindImpl.File(FqName(packageName)),
    )

    private fun String.relativeToPackage() = getPackageFqName().child(Name.identifier(this))


    override fun getPackageFqName(): FqName = when (val k = kind) {
        is CangJieFileStubKind.WithPackage -> k.packageFqName
        is CangJieFileStubKind.Invalid -> FqName.ROOT
    }

    override fun getType(): IStubFileElementType<CangJieFileStub> = CjFileElementType.INSTANCE

    override fun toString(): String = "CangJieFileStubImpl[$kind]"
    override fun copyInto(newParent: StubElement<*>?): CangJieFileStubImpl = CangJieFileStubImpl(
        file = null, // no psi should be copied
        kind = kind,
    )

    companion object {
        /**
         * 创建普通源文件的 Stub。
         */
        fun forFile(packageFqName: FqName): CangJieFileStubImpl = CangJieFileStubImpl(
            file = null,
            kind = CangJieFileStubKindImpl.File(packageFqName),
        )

        /**
         * 创建 Facade 文件的 Stub。
         */
        fun forFileFacadeStub(facadeFqName: FqName): CangJieFileStubImpl = CangJieFileStubImpl(
            file = null,
            kind = CangJieFileStubKindImpl.Facade(
                packageFqName = facadeFqName.parent(),
                facadeFqName = facadeFqName,
            ),
        )

        /**
         * 创建无效文件的 Stub（解析失败等情况）。
         */
        fun forInvalid(errorMessage: String): CangJieFileStubImpl = CangJieFileStubImpl(
            file = null,
            kind = CangJieFileStubKindImpl.Invalid(errorMessage),
        )

        /**
         * 创建多文件类 Facade 的 Stub。
         */
        fun forMultifileClassStub(
            packageFqName: FqName,
            facadeFqName: FqName,
            partNames: List<String>?
        ): CangJieFileStubImpl = CangJieFileStubImpl(
            file = null,
            kind = CangJieFileStubKindImpl.MultifileClass(
                packageFqName = packageFqName,
                facadeFqName = facadeFqName,
                facadePartSimpleNames = partNames ?: emptyList(),
            ),
        )
    }
}

/**
 * 通用 stub 深拷贝。
 *
 * 不能再依赖零散的 `copyInto()` 手写实现，因为 decompiled file 会引入：
 * - package/import placeholder stub
 * - import directive stub
 * - extend stub
 * 等更多 compiled 视图子节点。
 *
 * 这里统一走 stub serializer / deserializer 链路复制整棵树，
 * 保证所有可序列化 stub 在 source / decompiled 两条链上都能稳定复用。
 */
private fun <P : PsiElement, S : StubElement<P>> serializeAndDeserializeStub(
    originalStub: S,
    deserializedParentStub: StubElement<*>?,
    buffer: UnsyncByteArrayOutputStream,
    storage: CangJieStringEnumerator,
): S {
    buffer.reset()

    @Suppress("DEPRECATION")
    val serializer = if (originalStub is com.intellij.psi.stubs.PsiFileStub<*>) {
        originalStub.type
    } else {
        originalStub.stubType
    }

    @Suppress("UNCHECKED_CAST")
    serializer as ObjectStubSerializer<StubElement<*>, StubElement<*>>

    serializer.serialize(originalStub, StubOutputStream(buffer, storage))

    val stubInputStream = StubInputStream(buffer.toInputStream(), storage)
    val deserializedStub = serializer.deserialize(stubInputStream, deserializedParentStub)
    checkWithAttachment(
        stubInputStream.read() == -1,
        { "Deserializer must consume the same number of bytes as serializer writes" },
    )
    checkWithAttachment(
        originalStub::class == deserializedStub::class,
        { "${originalStub::class.simpleName} is expected, but ${deserializedStub::class.simpleName} is found" },
    )

    for (originalChild in originalStub.childrenStubs) {
        serializeAndDeserializeStub(
            originalStub = originalChild,
            deserializedParentStub = deserializedStub,
            buffer = buffer,
            storage = storage,
        )
    }

    @Suppress("UNCHECKED_CAST")
    return deserializedStub as S
}

fun CangJieFileStubImpl.deepCopy(): CangJieFileStubImpl = serializeAndDeserializeStub(
    originalStub = this,
    deserializedParentStub = null,
    buffer = UnsyncByteArrayOutputStream(),
    storage = CangJieStringEnumerator(),
) as CangJieFileStubImpl

private class CangJieStringEnumerator : AbstractStringEnumerator {
    private val values = HashMap<String, Int>()
    private val strings = mutableListOf<String>()

    override fun enumerate(value: String?): Int {
        if (value == null) return 0
        return values.getOrPut(value) {
            strings += value
            values.size + 1
        }
    }

    override fun valueOf(idx: Int): String? = if (idx == 0) null else strings[idx - 1]

    override fun markCorrupted() = Unit

    override fun close() = Unit

    override fun isDirty(): Boolean = false

    override fun force() = Unit
}
