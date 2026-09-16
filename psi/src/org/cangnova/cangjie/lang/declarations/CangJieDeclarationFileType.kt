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

/**
 * `.cj.d` 声明文件类型。
 *
 * 复用 [org.cangnova.cangjie.lang.CangJieLanguage]，
 * 因此词法、语法、PSI 以及全部 language 级扩展（高亮、折叠、注释、括号匹配）
 * 无需重复注册即可生效。
 *
 * 必须继承 [CangJieFileType]（而非直接实现 `LanguageFileType`）：PSI 文件的创建分派按
 * `is CangJieFileType` 判定，脱离该层次会让 `.cj.d` 退化成纯文本 PSI。
 */
object CangJieDeclarationFileType : CangJieFileType() {
    /** 官方 `CJ_D_FILE_EXTENSION`。 */
    const val DECLARATION_EXTENSION: String = "cj.d"

    /**
     * 文件类型唯一名。
     *
     * 必须与 `CangJie` FileType 区分：`FileTypeRegistry` 要求名称唯一，
     * 而基类实现返回 `CangJieLanguage.displayName`（即 `"CangJie"`）。
     */
    override fun getName(): String = "CangJieDeclaration"

    override fun getDescription(): String = "CangJie declaration file"

    override fun getDefaultExtension(): String = DECLARATION_EXTENSION
}
