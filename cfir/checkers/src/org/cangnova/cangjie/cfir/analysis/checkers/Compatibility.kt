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

/** 两个修饰符同时出现时的兼容性分类。 */
internal enum class Compatibility {
    /** 两个修饰符可以同时出现。 */
    COMPATIBLE,
    /** 第二个修饰符被第一个修饰符覆盖，属于冗余。 */
    REDUNDANT,
    /** 第一个修饰符被第二个修饰符覆盖，属于反向冗余。 */
    REVERSE_REDUNDANT,
    /** 两个修饰符是同一个 token，属于重复。 */
    REPEATED,
    /** 该修饰符组合仍可接受，但需要报告弃用组合。 */
    DEPRECATED,
    /** 两个修饰符不能同时出现。 */
    INCOMPATIBLE,
    /** 该组合只允许出现在 class-like 声明上。 */
    COMPATIBLE_FOR_CLASSES_ONLY,
}

/** 全量互斥/冗余/兼容特例表。 */
private val mutualCompatibility = buildCompatibilityMap()

/** 构建修饰符两两兼容性表。 */
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

/** 查询两个修饰符 token 的兼容性分类。 */
internal fun compatibility(first: CjKeywordToken, second: CjKeywordToken): Compatibility {
    return if (first == second) {
        Compatibility.REPEATED
    } else {
        mutualCompatibility[first to second] ?: Compatibility.COMPATIBLE
    }
}

/** 注册只在 class-like 声明上允许共存的修饰符组合。 */
private fun compatibilityForClassesRegister(vararg list: CjKeywordToken): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    return compatibilityRegister(Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, *list)
}

/** 为给定修饰符列表中的任意两个不同 token 注册同一种兼容性分类。 */
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

/** 注册一组两两不兼容的修饰符。 */
private fun incompatibilityRegister(vararg list: CjKeywordToken): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    return compatibilityRegister(Compatibility.INCOMPATIBLE, *list)
}

/** 注册一个有方向的冗余关系。 */
private fun redundantRegister(
    sufficient: CjKeywordToken,
    redundant: CjKeywordToken,
): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    return mapOf(
        (sufficient to redundant) to Compatibility.REDUNDANT,
        (redundant to sufficient) to Compatibility.REVERSE_REDUNDANT,
    )
}

/** 注册一个对称的弃用修饰符组合。 */
private fun deprecatedRegister(
    first: CjKeywordToken,
    second: CjKeywordToken,
): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    return mapOf(
        (first to second) to Compatibility.DEPRECATED,
        (second to first) to Compatibility.DEPRECATED,
    )
}
