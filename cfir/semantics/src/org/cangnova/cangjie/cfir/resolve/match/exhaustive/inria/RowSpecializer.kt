package org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria

import org.cangnova.cangjie.cfir.resolve.match.CfirConstructor
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * Maranget 算法中的行特化器。
 *
 * 根据当前分析构造器把矩阵行首模式展开为 payload 子模式，并保留行尾。
 */
object RowSpecializer {
    /**
     * 按 [constructor] 特化单行模式。
     *
     * @return 特化后的行；返回 `null` 表示该行不能覆盖当前构造器。
     */
    fun specializeRow(
        row: List<CfirMatchPattern>,
        constructor: CfirConstructor,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
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

    /**
     * 用真实子模式替换构造器 payload 通配占位。
     */
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
        context: MatchExhaustivenessContext,
    ): Boolean = AbstractTypeChecker.isSubtypeOf(context.session.typeContext, this, patternType) == true
}
