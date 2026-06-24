package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.inria

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirConstructor
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker

/** Maranget 算法的行特化工具，负责按选定构造器重写矩阵行。 */
object RowSpecializer {
    /** 将一行模式按照目标构造器特化为子模式行；无法覆盖该构造器时返回空。 */
    fun specializeRow(
        row: List<CfirMatchPattern>,
        constructor: CfirConstructor,
        type: ConeCangJieType,
        context: CheckerContext,
    ): List<CfirMatchPattern>? {
        val firstPattern = row.firstOrNull() ?: return emptyList()

        val wildPatterns = constructor.subTypes(type)
            .map { subType -> CfirMatchPattern.wild(subType) }
            .toMutableList()

        val head: List<CfirMatchPattern>? = when (val kind = firstPattern.kind) {
            is CfirMatchPatternKind.Enum -> {
                if (constructor == firstPattern.constructors.firstOrNull()) {
                    wildPatterns.apply { fillWithSubPatterns(kind.subPatterns) }
                } else {
                    null
                }
            }
            is CfirMatchPatternKind.Tuple -> wildPatterns.apply { fillWithSubPatterns(kind.subPatterns) }
            is CfirMatchPatternKind.Const -> {
                if (constructor.coveredByRange(kind.value, kind.value, included = true)) emptyList() else null
            }
            is CfirMatchPatternKind.Type -> {
                val constructorType = (constructor as? CfirConstructor.Type)?.type
                if (constructorType != null && constructorType.isCoveredByTypePattern(kind.type, context)) {
                    wildPatterns
                } else {
                    null
                }
            }
            CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> wildPatterns
            CfirMatchPatternKind.Error -> null
        }

        return head?.plus(row.subList(1, row.size))
    }

    /** 用显式子模式覆盖构造器形参对应的默认通配符槽位。 */
    private fun MutableList<CfirMatchPattern>.fillWithSubPatterns(subPatterns: List<CfirMatchPattern>) {
        for ((index, pattern) in subPatterns.withIndex()) {
            while (size <= index) add(CfirMatchPattern.wild())
            this[index] = pattern
        }
    }

    /**
     * 官方 `Constructor::IsCoveredBy` 对 TYPE 构造器使用 `candidateTy <: patternTy`。
     * 这让 `case _: Collection<T>` 覆盖后续 `case _: Array<T>`，但不会把开放父类型
     * 的 selector 提前收窄成同型判断。
     */
    private fun ConeCangJieType.isCoveredByTypePattern(
        patternType: ConeCangJieType,
        context: CheckerContext,
    ): Boolean = AbstractTypeChecker.isSubtypeOf(context.session.typeContext, this, patternType) == true
}
