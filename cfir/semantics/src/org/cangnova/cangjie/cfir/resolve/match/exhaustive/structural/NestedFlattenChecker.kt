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

package org.cangnova.cangjie.cfir.resolve.match.exhaustive.structural

import org.cangnova.cangjie.cfir.declarations.expandedPatternEnumType
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.collectEnumConstructorNames
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType

/**
 * 嵌套 enum 模式的展开式穷尽性检查器。
 *
 * 该 checker 将可控深度内的嵌套 enum 模式压平成字符串路径，再用位图检查覆盖情况。
 */
class NestedFlattenChecker : ExhaustivenessChecker {
    /** 当前 checker 来源。 */
    override val source: CheckSource = CheckSource.NESTED_FLATTEN

    /** 当前 checker 优先级。 */
    override val priority: Int = 50

    /** 最大展开嵌套深度。 */
    private val maxNestingDepth = 3

    /** 最大可展开构造器路径数量。 */
    private val maxFlattenedConstructors = 64

    /** 只处理可展开路径数量在上限内的 enum 类型。 */
    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): Boolean {
        val enumType = type.expandedPatternEnumType(context.session) ?: return false
        val flattenedCount = estimateFlattenedConstructors(enumType, context, 0)
        return flattenedCount in 1..maxFlattenedConstructors
    }

    /** 执行嵌套 enum 展开覆盖检查。 */
    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult {
        val enumType = type.expandedPatternEnumType(context.session) ?: return ExhaustivenessResult.Skipped
        val flattenedPatterns = matrix.mapNotNull { row -> row.firstOrNull()?.let(::flattenPattern) }
        if (flattenedPatterns.isEmpty()) return ExhaustivenessResult.Skipped

        val allPaths = collectAllPaths(enumType, context, 0)
        if (allPaths.size > maxFlattenedConstructors) return ExhaustivenessResult.Skipped

        val indexByPath = allPaths.withIndex().associate { (index, path) -> path to index }
        val allMask = (1L shl allPaths.size) - 1
        var covered = 0L

        for (flat in flattenedPatterns) {
            when (flat) {
                FlattenedPattern.Wildcard -> covered = allMask
                is FlattenedPattern.Path -> {
                    val idx = indexByPath[flat.path]
                    if (idx != null) covered = covered or (1L shl idx)
                }

                is FlattenedPattern.Prefix -> {
                    for ((path, idx) in indexByPath) {
                        if (path.startsWith(flat.prefix)) covered = covered or (1L shl idx)
                    }
                }
            }
            if (covered == allMask) return ExhaustivenessResult.Exhaustive
        }

        return if (covered == allMask) {
            ExhaustivenessResult.Exhaustive
        } else {
            val missingPaths = allPaths.filterIndexed { idx, _ -> (covered and (1L shl idx)) == 0L }
            val missingPatterns = missingPaths.take(5).map { reconstructPattern(type, it) }
            ExhaustivenessResult.NonExhaustive(missingPatterns, source)
        }
    }

    /** 粗略估算展开后的构造器路径数量。 */
    private fun estimateFlattenedConstructors(
        type: ConeEnumType,
        context: MatchExhaustivenessContext,
        depth: Int,
    ): Int {
        if (depth >= maxNestingDepth) return 1
        val constructors = collectEnumConstructorNames(type, context)
        if (constructors.isEmpty()) return 1
        return constructors.size
    }

    /** 收集当前实现可识别的全部展开路径。 */
    private fun collectAllPaths(
        type: ConeEnumType,
        context: MatchExhaustivenessContext,
        depth: Int,
    ): List<String> {
        if (depth >= maxNestingDepth) return listOf("")
        val constructors = collectEnumConstructorNames(type, context)
        if (constructors.isEmpty()) return listOf("")
        return constructors
    }

    /** 将规范化模式压平成路径表示。 */
    private fun flattenPattern(pattern: CfirMatchPattern): FlattenedPattern {
        return when (val kind = pattern.kind) {
            CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> FlattenedPattern.Wildcard
            is CfirMatchPatternKind.Enum -> {
                val name = kind.entryName
                if (kind.subPatterns.isEmpty()) {
                    FlattenedPattern.Path(name)
                } else if (kind.subPatterns.all { it.kind == CfirMatchPatternKind.Wild || it.kind is CfirMatchPatternKind.Binding }) {
                    FlattenedPattern.Prefix("$name(")
                } else {
                    val subStrings = kind.subPatterns.map {
                        when (val flat = flattenPattern(it)) {
                            FlattenedPattern.Wildcard -> "_"
                            is FlattenedPattern.Path -> flat.path
                            is FlattenedPattern.Prefix -> "${flat.prefix}..."
                        }
                    }
                    FlattenedPattern.Path("$name(${subStrings.joinToString(",")})")
                }
            }

            else -> FlattenedPattern.Wildcard
        }
    }

    /** 根据缺失路径恢复诊断用缺失模式。 */
    private fun reconstructPattern(type: ConeCangJieType, path: String): CfirMatchPattern {
        return CfirMatchPattern.wild(type)
    }

    /** 单例实例。 */
    companion object {
        /** 默认嵌套展开 checker 实例。 */
        val INSTANCE = NestedFlattenChecker()
    }
}

/**
 * 嵌套模式压平后的路径形态。
 */
private sealed class FlattenedPattern {
    /** 覆盖全部路径的通配形态。 */
    data object Wildcard : FlattenedPattern()

    /**
     * 精确路径形态。
     *
     * @property path 展开后的路径字符串。
     */
    data class Path(val path: String) : FlattenedPattern()

    /**
     * 前缀覆盖形态。
     *
     * @property prefix 展开路径前缀。
     */
    data class Prefix(val prefix: String) : FlattenedPattern()
}
