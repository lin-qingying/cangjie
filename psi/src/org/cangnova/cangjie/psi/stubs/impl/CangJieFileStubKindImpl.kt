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

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import kotlin.reflect.KProperty1

/**
 * 仓颉文件 Stub 类型的内部实现。
 *
 * 使用密封类来确保类型安全，并提供序列化/反序列化支持。
 */
  sealed class CangJieFileStubKindImpl {

    /**
     * 普通源文件。
     */
    data class File(override val packageFqName: FqName) : CangJieFileStubKindImpl(), CangJieFileStubKind.WithPackage.File {
        /**
         * 实现 `toString` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun toString(): String = toStringGenerator(File::packageFqName)
    }

    /**
     * 简单的 Facade 文件。
     */
    data class Facade(
        /**
         * 暴露 `packageFqName`，实现PSI Stub节点对上层接口的属性契约。
         */
        override val packageFqName: FqName,
        /**
         * 暴露 `facadeFqName`，实现PSI Stub节点对上层接口的属性契约。
         */
        override val facadeFqName: FqName,
    ) : CangJieFileStubKindImpl(), CangJieFileStubKind.WithPackage.Facade.Simple {
        /**
         * 暴露 `partSimpleName`，实现PSI Stub节点对上层接口的属性契约。
         */
        override val partSimpleName: String
            get() = facadeFqName.shortName().asString()

        /**
         * 实现 `toString` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun toString(): String = toStringGenerator(Facade::packageFqName, Facade::facadeFqName)
    }

    /**
     * 多文件类的 Facade。
     */
    data class MultifileClass(
        /**
         * 暴露 `packageFqName`，实现PSI Stub节点对上层接口的属性契约。
         */
        override val packageFqName: FqName,
        /**
         * 暴露 `facadeFqName`，实现PSI Stub节点对上层接口的属性契约。
         */
        override val facadeFqName: FqName,
        /**
         * 暴露 `facadePartSimpleNames`，实现PSI Stub节点对上层接口的属性契约。
         */
        override val facadePartSimpleNames: List<String>,
    ) : CangJieFileStubKindImpl(), CangJieFileStubKind.WithPackage.Facade.MultifileClass {
        /**
         * 实现 `toString` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun toString(): String = toStringGenerator(
            MultifileClass::packageFqName,
            MultifileClass::facadeFqName,
            MultifileClass::facadePartSimpleNames,
        )
    }

    /**
     * 无效的文件 Stub。
     */
    data class Invalid(override val errorMessage: String) : CangJieFileStubKindImpl(), CangJieFileStubKind.Invalid {
        /**
         * 实现 `toString` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun toString(): String = toStringGenerator(Invalid::errorMessage)
    }

    companion object {
        /**
         * 将 Stub 类型序列化到输出流。
         *
         * @param kind 要序列化的 Stub 类型
         * @param dataStream 输出流
         */
        fun serialize(kind: CangJieFileStubKind, dataStream: StubOutputStream) {
            kind as CangJieFileStubKindImpl
            when (kind) {
                is File -> {
                    dataStream.writeByte(0)
                    dataStream.writeName(kind.packageFqName.asString())
                }

                is Facade -> {
                    dataStream.writeByte(2)
                    dataStream.writeName(kind.packageFqName.asString())
                    dataStream.writeName(kind.facadeFqName.asString())
                }

                is MultifileClass -> {
                    dataStream.writeByte(3)
                    dataStream.writeName(kind.packageFqName.asString())
                    dataStream.writeName(kind.facadeFqName.asString())
                    dataStream.writeVarInt(kind.facadePartSimpleNames.size)
                    kind.facadePartSimpleNames.forEach(dataStream::writeName)
                }

                is Invalid -> {
                    dataStream.writeByte(4)
                    dataStream.writeName(kind.errorMessage)
                }
            }
        }

        /**
         * 从输入流反序列化 Stub 类型。
         *
         * @param dataStream 输入流
         * @return 反序列化后的 Stub 类型
         */
        fun deserialize(dataStream: StubInputStream): CangJieFileStubKind =
            when (val kind = dataStream.readByte().toInt()) {
                0 -> {
                    val packageFqName = dataStream.readFqName()
                    File(packageFqName = packageFqName)
                }

                2 -> {
                    val packageFqName = dataStream.readFqName()
                    val facadeFqName = dataStream.readFqName()
                    Facade(packageFqName = packageFqName, facadeFqName = facadeFqName)
                }

                3 -> {
                    val packageFqName = dataStream.readFqName()
                    val facadeFqName = dataStream.readFqName()
                    val size = dataStream.readVarInt()
                    val facadePartSimpleNames = List(size) { dataStream.readNameString()!! }
                    MultifileClass(
                        packageFqName = packageFqName,
                        facadeFqName = facadeFqName,
                        facadePartSimpleNames = facadePartSimpleNames,
                    )
                }

                4 -> {
                    val errorMessage = dataStream.readNameString()!!
                    Invalid(errorMessage)
                }

                else -> error("Unknown file stub kind: $kind")
            }

        /**
         * 从输入流读取 FqName。
         */
        private fun StubInputStream.readFqName(): FqName {
            val name = readNameString() ?: ""
            return FqName(name)
        }
    }
}

/**
 * 生成 toString 表示的辅助方法。
 */
private fun <T : CangJieFileStubKind> T.toStringGenerator(vararg property: KProperty1<T, Any>): String {
    return property.joinToString(prefix = "${this::class.simpleName}[", postfix = "]", separator = ", ") {
        "${it.name}=${it.get(this)}"
    }
}
