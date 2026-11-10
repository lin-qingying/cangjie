/*
 * Copyright 2025 LinQingYing. and contributors.
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


package org.cangnova.cangjie.formatter

import org.cangnova.cangjie.lang.CangJieLanguage
import com.intellij.application.options.CodeStyle
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings


/**
 * 从通用 CodeStyleSettings 中取得仓颉 common settings。
 */
val CodeStyleSettings.cangjieCommonSettings: CangJieCommonCodeStyleSettings
    get() = getCommonSettings(CangJieLanguage) as CangJieCommonCodeStyleSettings

/**
 * 从通用 CodeStyleSettings 中取得仓颉 custom settings。
 */
val CodeStyleSettings.cangjieCustomSettings: CangJieCodeStyleSettings
    get() = getCustomSettings(CangJieCodeStyleSettings::class.java)

/**
 * 返回 custom/common settings 中一致的预定义仓颉代码风格标识。
 */
fun CodeStyleSettings.cangjieCodeStyleDefaults(): String? = cangjieCustomSettings.CODE_STYLE_DEFAULTS?.takeIf { customStyleId ->
    customStyleId == cangjieCommonSettings.CODE_STYLE_DEFAULTS
}

/**
 * 返回当前设置推断出的仓颉预定义代码风格标识。
 */
fun CodeStyleSettings.supposedCangJieCodeStyleDefaults(): String? =
    cangjieCustomSettings.CODE_STYLE_DEFAULTS ?: cangjieCommonSettings.CODE_STYLE_DEFAULTS

/**
 * 从 PSI 文件取得仓颉 common settings。
 */
val PsiFile.cangjieCommonSettings: CangJieCommonCodeStyleSettings get() = CodeStyle.getSettings(this).cangjieCommonSettings
/**
 * 从 PSI 文件取得仓颉 custom settings。
 */
val PsiFile.cangjieCustomSettings: CangJieCodeStyleSettings get() = CodeStyle.getSettings(this).cangjieCustomSettings
/**
 * 返回 PSI 文件当前代码风格中的仓颉右边距。
 */
val PsiFile.rightMarginOrDefault: Int get() = CodeStyle.getSettings(this).getRightMargin(CangJieLanguage)
