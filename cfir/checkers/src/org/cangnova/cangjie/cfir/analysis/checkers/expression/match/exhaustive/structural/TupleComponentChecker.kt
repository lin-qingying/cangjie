package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.structural

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.specialized.BooleanChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.specialized.SmallEnumBitVectorChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.trivial.TrivialChecker
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTupleType

/** 将 tuple 模式拆分为按分量独立检查的结构化穷尽性 checker。 */
class TupleComponentChecker(
    /** 用于分析每个 tuple 分量的专项 checker 列表，按传入顺序尝试。 */
    private val componentCheckers: List<ExhaustivenessChecker>,
) : ExhaustivenessChecker {
    /** 当前 checker 在穷尽性结果中使用的来源标记。 */
    override val source: CheckSource = CheckSource.TUPLE_COMPONENT

    /** tuple 分量 checker 的调度优先级。 */
    override val priority: Int = 40

    /** 仅当目标类型是 tuple 且每个模式都可独立拆分时启用。 */
    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: CheckerContext,
    ): Boolean {
        if (type !is ConeTupleType) return false
        return patterns.all { pattern ->
            when (val kind = pattern.kind) {
                CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> true
                is CfirMatchPatternKind.Tuple -> kind.subPatterns.all(::isSimplePattern)
                else -> false
            }
        }
    }

    /** 把矩阵按 tuple 分量拆成多列，分别检查后再合成一个缺失 tuple 模式。 */
    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: CheckerContext,
    ): ExhaustivenessResult {
        val tupleType = type as? ConeTupleType ?: return ExhaustivenessResult.Skipped
        val elementTypes = tupleType.elementTypes
        val arity = elementTypes.size

        if (arity == 0) {
            return if (matrix.isNotEmpty()) {
                ExhaustivenessResult.Exhaustive
            } else {
                ExhaustivenessResult.NonExhaustive(listOf(CfirMatchPattern.wild(type)), source)
            }
        }

        val columnPatterns = (0 until arity).map { col ->
            extractColumnPatterns(matrix, col, elementTypes[col])
        }

        if (!canAnalyzeIndependently(columnPatterns)) return ExhaustivenessResult.Skipped

        val columnResults = columnPatterns.mapIndexed { col, patterns ->
            val columnType = elementTypes[col]
            val columnMatrix = patterns.map { listOf(it) }
            checkColumn(columnMatrix, columnType, context)
        }

        return combineResults(columnResults, elementTypes, type)
    }

    /** 从矩阵首列中抽取指定 tuple 分量对应的模式列。 */
    private fun extractColumnPatterns(
        matrix: CfirMatrix,
        columnIndex: Int,
        elementType: ConeCangJieType,
    ): List<CfirMatchPattern> {
        return matrix.mapNotNull { row ->
            val first = row.firstOrNull() ?: return@mapNotNull null
            when (val kind = first.kind) {
                CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> CfirMatchPattern.wild(elementType)
                is CfirMatchPatternKind.Tuple -> kind.subPatterns.getOrNull(columnIndex) ?: CfirMatchPattern.wild(elementType)
                else -> null
            }
        }
    }

    /** 判断每个 tuple 分量列是否都有可分析模式。 */
    private fun canAnalyzeIndependently(columnPatterns: List<List<CfirMatchPattern>>): Boolean =
        columnPatterns.all { it.isNotEmpty() }

    /** 对单个 tuple 分量列选择第一个可用 checker 并返回其穷尽性结果。 */
    private fun checkColumn(
        columnMatrix: CfirMatrix,
        columnType: ConeCangJieType,
        context: CheckerContext,
    ): ExhaustivenessResult {
        val patterns = columnMatrix.flatten()
        for (checker in componentCheckers) {
            if (checker.isApplicable(columnType, patterns, context)) {
                val result = checker.check(columnMatrix, columnType, context)
                if (result !is ExhaustivenessResult.Skipped) return result
            }
        }
        return ExhaustivenessResult.Skipped
    }

    /** 将各分量结果合成为 tuple 级别结果，并构造首个缺失 tuple witness。 */
    private fun combineResults(
        columnResults: List<ExhaustivenessResult>,
        elementTypes: List<ConeCangJieType>,
        tupleType: ConeCangJieType,
    ): ExhaustivenessResult {
        if (columnResults.any { it is ExhaustivenessResult.Skipped }) return ExhaustivenessResult.Skipped
        val error = columnResults.filterIsInstance<ExhaustivenessResult.Error>().firstOrNull()
        if (error != null) return error
        if (columnResults.all { it is ExhaustivenessResult.Exhaustive }) return ExhaustivenessResult.Exhaustive

        val missingByColumn = columnResults.mapIndexed { index, result ->
            when (result) {
                is ExhaustivenessResult.NonExhaustive -> result.missingPatterns
                else -> listOf(CfirMatchPattern.wild(elementTypes[index]))
            }
        }
        val firstMissingTuple = missingByColumn.map { it.firstOrNull() ?: CfirMatchPattern.wild() }
        val missingTuple = CfirMatchPattern(tupleType, CfirMatchPatternKind.Tuple(firstMissingTuple), null)
        return ExhaustivenessResult.NonExhaustive(listOf(missingTuple), source)
    }

    /** 判断模式是否足够简单，可在 tuple 分量维度独立分析。 */
    private fun isSimplePattern(pattern: CfirMatchPattern): Boolean = when (val kind = pattern.kind) {
        CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> true
        is CfirMatchPatternKind.Const -> true
        is CfirMatchPatternKind.Enum -> kind.subPatterns.all(::isSimplePattern)
        is CfirMatchPatternKind.Type -> true
        else -> false
    }

    /** tuple 分量 checker 的默认实例工厂与共享单例容器。 */
    companion object {
        /** 创建使用基础、布尔和小型 enum checker 的默认 tuple 分量 checker。 */
        fun createDefault(): TupleComponentChecker {
            return TupleComponentChecker(
                listOf(
                    TrivialChecker.INSTANCE,
                    BooleanChecker.INSTANCE,
                    SmallEnumBitVectorChecker.INSTANCE,
                )
            )
        }

        /** 默认 tuple 分量 checker 单例。 */
        val INSTANCE by lazy { createDefault() }
    }
}
