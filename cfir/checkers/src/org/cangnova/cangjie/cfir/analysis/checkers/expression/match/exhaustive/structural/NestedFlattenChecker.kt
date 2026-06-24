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

package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.structural

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.collectEnumConstructorNames
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.declarations.expandedPatternEnumType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType

/** 将有限深度的嵌套 enum 模式压平为路径后执行覆盖判断的结构化 checker。 */
class NestedFlattenChecker : ExhaustivenessChecker {
    /** 当前 checker 在穷尽性结果中使用的来源标记。 */
    override val source: CheckSource = CheckSource.NESTED_FLATTEN

    /** 嵌套压平 checker 的调度优先级。 */
    override val priority: Int = 50

    /** 为避免组合爆炸而允许展开的最大嵌套深度。 */
    private val maxNestingDepth = 3

    /** 位掩码覆盖模型允许记录的最大压平构造器路径数量。 */
    private val maxFlattenedConstructors = 64

    /** 仅对可枚举且压平路径数量在容量内的 enum 类型启用。 */
    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: CheckerContext,
    ): Boolean {
        val enumType = type.expandedPatternEnumType(context.session) ?: return false
        val flattenedCount = estimateFlattenedConstructors(enumType, context, 0)
        return flattenedCount in 1..maxFlattenedConstructors
    }

    /** 将矩阵首列模式压平为路径集合，并通过位掩码判断全部路径是否已覆盖。 */
    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: CheckerContext,
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

    /** 估算给定 enum 在当前深度下会产生的压平构造器路径数量。 */
    private fun estimateFlattenedConstructors(
        type: ConeEnumType,
        context: CheckerContext,
        depth: Int,
    ): Int {
        if (depth >= maxNestingDepth) return 1
        val constructors = collectEnumConstructorNames(type, context)
        if (constructors.isEmpty()) return 1
        return constructors.size
    }

    /** 收集给定 enum 在当前深度下参与覆盖比较的全部路径标识。 */
    private fun collectAllPaths(
        type: ConeEnumType,
        context: CheckerContext,
        depth: Int,
    ): List<String> {
        if (depth >= maxNestingDepth) return listOf("")
        val constructors = collectEnumConstructorNames(type, context)
        if (constructors.isEmpty()) return listOf("")
        return constructors
    }

    /** 将一个嵌套模式转换为通配符、精确路径或路径前缀三类压平形态。 */
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

    /** 根据缺失路径构造对外报告的缺失模式；当前以目标类型通配符表达该路径缺口。 */
    private fun reconstructPattern(type: ConeCangJieType, path: String): CfirMatchPattern {
        return CfirMatchPattern.wild(type)
    }

    /** 嵌套压平 checker 的共享单例容器。 */
    companion object {
        /** 默认嵌套压平 checker 单例。 */
        val INSTANCE = NestedFlattenChecker()
    }
}

/** 压平后的模式形态，用于把嵌套 enum 覆盖问题转换为路径覆盖问题。 */
private sealed class FlattenedPattern {
    /** 表示当前模式覆盖目标类型下的全部路径。 */
    data object Wildcard : FlattenedPattern()

    /**
     * 表示一个精确构造器路径。
     *
     * @property path 已压平的构造器路径标识。
     */
    data class Path(val path: String) : FlattenedPattern()

    /**
     * 表示一个构造器路径前缀，覆盖此前缀下的全部子路径。
     *
     * @property prefix 已压平的构造器路径前缀。
     */
    data class Prefix(val prefix: String) : FlattenedPattern()
}
