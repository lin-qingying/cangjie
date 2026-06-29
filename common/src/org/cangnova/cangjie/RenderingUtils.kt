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

package org.cangnova.cangjie

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import kotlin.text.contains

/**
 * 判断标识符字符串在渲染时是否需要反引号转义。
 */
private fun shouldBeEscaped(string: String): Boolean {
    return string in KeywordStringsGenerated.KEYWORDS ||
            string.any { !Character.isLetterOrDigit(it) && it != '_' } ||
            string.isEmpty() ||
            !Character.isJavaIdentifierStart(string.codePointAt(0))
}

/**
 * 将名称渲染为源码展示文本，必要时使用反引号转义。
 */
fun Name.render(stipSpecialMarkers: Boolean = false): String {
    val string = if (stipSpecialMarkers) asStringStripSpecialMarkers() else asString()
    return if ((!stipSpecialMarkers || !isSpecial) &&  shouldBeEscaped(string)) '`' + string + '`' else string
}
/**
 * 判断名称本身是否需要转义。
 */
private fun Name.shouldBeEscaped(): Boolean {
    val string = asString()
    return string in KeywordStringsGenerated.KEYWORDS ||
        string.any { !Character.isLetterOrDigit(it) && it != '_' } ||
        string.isEmpty() ||
        !Character.isJavaIdentifierStart(string.codePointAt(0))
}

/**
 * 渲染 unsafe FqName。
 */
fun FqNameUnsafe.render(): String {
    return renderFqName(pathSegments())
}

/**
 * 渲染安全 FqName。
 */
fun FqName.render(): String {
    return renderFqName(pathSegments())
}

/**
 * 渲染点分隔限定名路径。
 */
fun renderFqName(pathSegments: List<Name>): String {
    return buildString {
        for (element in pathSegments) {
            if (length > 0) {
                append(".")
            }
            append(element.render())
        }
    }
}

/**
 * 在上下界类型展示文本只有前缀不同或可空性不同的时候折叠前缀。
 */
fun replacePrefixesInTypeRepresentations(
    lowerRendered: String,
    lowerPrefix: String,
    upperRendered: String,
    upperPrefix: String,
    foldedPrefix: String,
): String? {
    if (lowerRendered.startsWith(lowerPrefix) && upperRendered.startsWith(upperPrefix)) {
        val lowerWithoutPrefix = lowerRendered.substring(lowerPrefix.length)
        val upperWithoutPrefix = upperRendered.substring(upperPrefix.length)
        val flexibleCollectionName = foldedPrefix + lowerWithoutPrefix

        if (lowerWithoutPrefix == upperWithoutPrefix) return flexibleCollectionName

        if (typeStringsDifferOnlyInNullability(lowerWithoutPrefix, upperWithoutPrefix)) {
            return "$flexibleCollectionName!"
        }
    }
    return null
}

/**
 * 判断两个类型展示文本是否只存在可空性差异。
 */
fun typeStringsDifferOnlyInNullability(lower: String, upper: String) =
    lower == upper.replace("?", "") || upper.endsWith("?") && ("$lower?") == upper || "($lower)?" == upper
