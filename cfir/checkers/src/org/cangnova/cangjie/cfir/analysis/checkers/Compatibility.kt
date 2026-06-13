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

package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens.*

internal enum class Compatibility {
    COMPATIBLE,
    REDUNDANT,
    REVERSE_REDUNDANT,
    REPEATED,
    DEPRECATED,
    INCOMPATIBLE,
    COMPATIBLE_FOR_CLASSES_ONLY,
}

private val mutualCompatibility = buildCompatibilityMap()

private fun buildCompatibilityMap(): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    val result = hashMapOf<Pair<CjKeywordToken, CjKeywordToken>, Compatibility>()

    result += incompatibilityRegister(PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD)

    result += redundantRegister(ABSTRACT_KEYWORD, OPEN_KEYWORD)
    result += redundantRegister(SEALED_KEYWORD, PUBLIC_KEYWORD)
    result += redundantRegister(SEALED_KEYWORD, OPEN_KEYWORD)

    result += incompatibilityRegister(CONST_KEYWORD, ABSTRACT_KEYWORD)
    result += incompatibilityRegister(CONST_KEYWORD, OPEN_KEYWORD)
    result += incompatibilityRegister(CONST_KEYWORD, MUT_KEYWORD)

    result += incompatibilityRegister(REDEF_KEYWORD, OVERRIDE_KEYWORD)
    result += incompatibilityRegister(STATIC_KEYWORD, OVERRIDE_KEYWORD)
    result += incompatibilityRegister(STATIC_KEYWORD, OPERATOR_KEYWORD)
    result += incompatibilityRegister(OPEN_KEYWORD, REDEF_KEYWORD)
    result += incompatibilityRegister(STATIC_KEYWORD, OPEN_KEYWORD)

    result += compatibilityForClassesRegister(PRIVATE_KEYWORD, OPEN_KEYWORD)
    result += compatibilityForClassesRegister(PRIVATE_KEYWORD, ABSTRACT_KEYWORD)

    return result
}

internal fun compatibility(first: CjKeywordToken, second: CjKeywordToken): Compatibility {
    return if (first == second) {
        Compatibility.REPEATED
    } else {
        mutualCompatibility[first to second] ?: Compatibility.COMPATIBLE
    }
}

private fun compatibilityForClassesRegister(vararg list: CjKeywordToken): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    return compatibilityRegister(Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, *list)
}

private fun compatibilityRegister(
    compatibility: Compatibility,
    vararg list: CjKeywordToken,
): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    val result = hashMapOf<Pair<CjKeywordToken, CjKeywordToken>, Compatibility>()
    for (first in list) {
        for (second in list) {
            if (first != second) {
                result[first to second] = compatibility
            }
        }
    }
    return result
}

private fun incompatibilityRegister(vararg list: CjKeywordToken): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    return compatibilityRegister(Compatibility.INCOMPATIBLE, *list)
}

private fun redundantRegister(
    sufficient: CjKeywordToken,
    redundant: CjKeywordToken,
): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    return mapOf(
        (sufficient to redundant) to Compatibility.REDUNDANT,
        (redundant to sufficient) to Compatibility.REVERSE_REDUNDANT,
    )
}

private fun deprecatedRegister(
    first: CjKeywordToken,
    second: CjKeywordToken,
): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    return mapOf(
        (first to second) to Compatibility.DEPRECATED,
        (second to first) to Compatibility.DEPRECATED,
    )
}
