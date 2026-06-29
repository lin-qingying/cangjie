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
 */

package org.cangnova.cangjie.lang

import com.intellij.lang.Language

/**
 * 仓颉宏展开文件的方言语言
 *
 * 作为 [CangJieLanguage] 的方言，用于 `.cj.macrocall` 文件。
 * 独立的语言定义使 `.macrocall` 文件拥有独立的 ParserDefinition，
 * 从而不构建 Stub 索引，避免与源文件产生 REDECLARATION 冲突。
 */
object CangJieMacroCallLanguage : Language(CangJieLanguage, "CangJieMacroCall") {
    /**
     * 执行 `readResolve` 内部辅助逻辑，支撑仓颉语言文件类型节点的结构解析与访问。
     */
    private fun readResolve(): Any = CangJieMacroCallLanguage

    /**
     * 实现 `isCaseSensitive` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isCaseSensitive() = true

    /**
     * 实现 `getDisplayName` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getDisplayName() = "CangJie Macro Call"
}
