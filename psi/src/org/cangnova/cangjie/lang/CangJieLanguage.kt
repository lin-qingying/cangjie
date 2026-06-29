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

package org.cangnova.cangjie.lang

import com.intellij.lang.Language


/**
 * 提供 `CangJieLanguage` 单例，集中承载仓颉语言文件类型的共享状态、工厂或工具行为。
 */
object CangJieLanguage : Language("CangJie") {
    /**
     * 执行 `readResolve` 内部辅助逻辑，支撑仓颉语言文件类型节点的结构解析与访问。
     */
    private fun readResolve(): Any = CangJieLanguage

    /**
     * 保存 `NAME`，供仓颉语言文件类型流程读取节点结构或语义信息。
     */
    val NAME: String = "CangJie"

//    private fun readResolve(): Any = CangJieLanguage

    /**
     * 实现 `isCaseSensitive` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isCaseSensitive() = true

    /**
     * 实现 `getDisplayName` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getDisplayName() = NAME
}
