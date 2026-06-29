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

/**
 * 表示 `CangJieStubOrigin`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
sealed class CangJieStubOrigin {
    companion object {
        private const val FACADE_KIND = 1
        private const val MULTI_FILE_FACADE_KIND = 2

        @JvmStatic
        fun serialize(origin: CangJieStubOrigin?, dataStream: StubOutputStream) {
            if (origin == null) {
                dataStream.writeInt(0)
            } else {
                dataStream.writeInt(origin.kind)
                origin.serializeContent(dataStream)
            }
        }

        @JvmStatic
        fun deserialize(dataStream: StubInputStream): CangJieStubOrigin? {
            return when (dataStream.readInt()) {
                FACADE_KIND -> Facade.deserializeContent(dataStream)
                MULTI_FILE_FACADE_KIND -> MultiFileFacade.deserializeContent(dataStream)
                else -> null
            }
        }
    }

    /**
     * 保存 `kind`，供PSI Stub流程读取节点结构或语义信息。
     */
    protected abstract val kind: Int

    /**
     * 提供 `serializeContent` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    protected abstract fun serializeContent(dataStream: StubOutputStream)

    /**
     * 表示 `Facade`，承载PSI Stub中的语法节点、索引桩或辅助模型。
     */
    data class Facade(
        /**
         * 保存 `className`，供PSI Stub流程读取节点结构或语义信息。
         */
        val className: String, // Internal name of the package part class
    ) : CangJieStubOrigin() {
        companion object {
            @JvmStatic
            internal fun deserializeContent(dataStream: StubInputStream): Facade? {
                val className = dataStream.readNameString() ?: return null
                return Facade(className)
            }
        }

        /**
         * 暴露 `kind`，实现PSI Stub节点对上层接口的属性契约。
         */
        override val kind: Int get() = FACADE_KIND

        /**
         * 实现 `serializeContent` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun serializeContent(dataStream: StubOutputStream) {
            dataStream.writeName(className)
        }
    }

    /**
     * 表示 `MultiFileFacade`，承载PSI Stub中的语法节点、索引桩或辅助模型。
     */
    data class MultiFileFacade(
        /**
         * 保存 `className`，供PSI Stub流程读取节点结构或语义信息。
         */
        val className: String, // Internal name of the package part class
        /**
         * 保存 `facadeClassName`，供PSI Stub流程读取节点结构或语义信息。
         */
        val facadeClassName: String, // Internal name of the facade class
    ) : CangJieStubOrigin() {
        companion object {
            @JvmStatic
            internal fun deserializeContent(dataStream: StubInputStream): MultiFileFacade? {
                val classId = dataStream.readNameString() ?: return null
                val facadeClassId = dataStream.readNameString() ?: return null
                return MultiFileFacade(classId, facadeClassId)
            }
        }

        /**
         * 暴露 `kind`，实现PSI Stub节点对上层接口的属性契约。
         */
        override val kind: Int get() = MULTI_FILE_FACADE_KIND

        /**
         * 实现 `serializeContent` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun serializeContent(dataStream: StubOutputStream) {
            dataStream.writeName(className)
            dataStream.writeName(facadeClassName)
        }
    }
}
