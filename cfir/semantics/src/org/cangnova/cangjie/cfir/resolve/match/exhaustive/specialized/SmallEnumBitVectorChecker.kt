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

package org.cangnova.cangjie.cfir.resolve.match.exhaustive.specialized

import org.cangnova.cangjie.cfir.declarations.expandedPatternEnumType
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.collectEnumConstructorNames
import org.cangnova.cangjie.cfir.resolve.match.isNonExhaustiveEnum
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 小 enum 类型的位图穷尽性检查器。
 *
 * enum 构造器数量不超过 [maxVariants] 时，用 Long bit mask 记录已覆盖 entry。
 */
class SmallEnumBitVectorChecker : ExhaustivenessChecker {
    /** 当前 checker 来源。 */
    override val source: CheckSource = CheckSource.ENUM_BITVECTOR

    /** 当前 checker 优先级。 */
    override val priority: Int = 20

    /** 当前位图实现可表示的最大 enum entry 数量。 */
    private val maxVariants = 64

    /** 只处理可展开且 entry 数量在位图范围内的 enum 类型。 */
    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): Boolean {
        val enumType = type.expandedPatternEnumType(context.session) ?: return false
        if (enumType.isNonExhaustiveEnum(context.session)) return false
        val variantCount = collectEnumConstructorNames(enumType, context).size
        return variantCount in 1..maxVariants
    }

    /** 执行 enum bit-vector 覆盖检查。 */
    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult {
        val enumType = type.expandedPatternEnumType(context.session) ?: return ExhaustivenessResult.Skipped
        val variants = collectEnumConstructorNames(enumType, context)
        val variantCount = variants.size
        if (variantCount == 0 || variantCount > maxVariants) return ExhaustivenessResult.Skipped

        val indexByVariant = variants.withIndex().associate { (index, name) -> name to index }
        val allMask = (1L shl variantCount) - 1
        var covered = 0L

        for (row in matrix) {
            val pattern = row.firstOrNull() ?: continue
            when (val kind = pattern.kind) {
                CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> covered = allMask
                is CfirMatchPatternKind.Enum -> {
                    if (kind.enumClassId == enumType.classId) {
                        val index = indexByVariant[kind.entryName]
                        if (index != null) covered = covered or (1L shl index)
                    }
                }

                else -> Unit
            }
            if (covered == allMask) return ExhaustivenessResult.Exhaustive
        }

        return if (covered == allMask) {
            ExhaustivenessResult.Exhaustive
        } else {
            val missing = variants.filterIndexed { index, _ ->
                (covered and (1L shl index)) == 0L
            }.map { missingEntry ->
                CfirMatchPattern(
                    enumType,
                    CfirMatchPatternKind.Enum(enumType.classId, missingEntry, emptyList()),
                    null,
                )
            }
            ExhaustivenessResult.NonExhaustive(missing, source)
        }
    }

    /** 单例实例。 */
    companion object {
        /** 默认小 enum bit-vector checker 实例。 */
        val INSTANCE = SmallEnumBitVectorChecker()
    }
}
