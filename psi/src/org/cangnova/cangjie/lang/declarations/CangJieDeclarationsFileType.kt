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

package org.cangnova.cangjie.lang.declarations

import org.cangnova.cangjie.lang.CangJieFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile

import javax.swing.Icon


/**
 * 提供 `CangJieBuiltInFileType` 单例，集中承载仓颉语言文件类型的共享状态、工厂或工具行为。
 */
object CangJieBuiltInFileType : FileType {
    /**
     * 实现 `getName` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName() = "cjo"

    /**
     * 实现 `getDescription` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getDescription(): String = ""

    /**
     * 实现 `getDefaultExtension` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getDefaultExtension() = "cjo"

    /**
     * 实现 `getIcon` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getIcon(): Icon? = null

    /**
     * 实现 `isBinary` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isBinary() = true

    /**
     * 实现 `isReadOnly` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isReadOnly() = true

    /**
     * 实现 `getCharset` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

    /**
     * 保存 `DEFAULT_DESCRIPTION`，供仓颉语言文件类型流程读取节点结构或语义信息。
     */
    private const val DEFAULT_DESCRIPTION = "CangJie built-in declarations"
}
