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

package org.cangnova.cangjie.messages

import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

/**
 * 保存 `PARSING_BUNDLE`，供PSI 模块流程读取节点结构或语义信息。
 */
@NonNls
const val PARSING_BUNDLE = "messages.CangJieParsingBundle"

/**
 * 提供 `CangJieParsingBundle` 单例，集中承载PSI 模块的共享状态、工厂或工具行为。
 */
object CangJieParsingBundle : AbstractCangJieBundle(PARSING_BUNDLE) {
    /**
     * 提供 `message` 操作，封装PSI 模块节点的访问、构造或判断逻辑。
     */
    @Nls
    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = PARSING_BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)

    /**
     * 提供 `htmlMessage` 操作，封装PSI 模块节点的访问、构造或判断逻辑。
     */
    @Nls
    @JvmStatic
    fun htmlMessage(@NonNls @PropertyKey(resourceBundle = PARSING_BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params).withHtml()

    /**
     * 提供 `lazyMessage` 操作，封装PSI 模块节点的访问、构造或判断逻辑。
     */
    @Nls
    @JvmStatic
    fun lazyMessage(@PropertyKey(resourceBundle = PARSING_BUNDLE) key: String, vararg params: Any): () -> String =
        { getMessage(key, *params) }
}
