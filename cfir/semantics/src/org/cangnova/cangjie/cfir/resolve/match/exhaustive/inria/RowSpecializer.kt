package org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria

import org.cangnova.cangjie.cfir.resolve.match.CfirConstructor
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.types.ConeCangJieType

object RowSpecializer {
    fun specializeRow(
        row: List<CfirMatchPattern>,
        constructor: CfirConstructor,
        type: ConeCangJieType,
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
                if (kind.type == type) wildPatterns else null
            }

            CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> wildPatterns
            CfirMatchPatternKind.Error -> null
        }

        return head?.plus(row.subList(1, row.size))
    }

    private fun MutableList<CfirMatchPattern>.fillWithSubPatterns(subPatterns: List<CfirMatchPattern>) {
        for ((index, pattern) in subPatterns.withIndex()) {
            while (size <= index) add(CfirMatchPattern.wild())
            this[index] = pattern
        }
    }
}
