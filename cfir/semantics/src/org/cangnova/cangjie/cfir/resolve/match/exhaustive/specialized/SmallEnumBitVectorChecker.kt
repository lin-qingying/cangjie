package org.cangnova.cangjie.cfir.resolve.match.exhaustive.specialized

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

class SmallEnumBitVectorChecker : ExhaustivenessChecker {
    override val source: CheckSource = CheckSource.ENUM_BITVECTOR
    override val priority: Int = 20
    private val maxVariants = 64

    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): Boolean {
        val enumType = type as? ConeEnumType ?: return false
        val variantCount = collectEnumConstructorNames(enumType, context).size
        return variantCount in 1..maxVariants
    }

    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult {
        val enumType = type as? ConeEnumType ?: return ExhaustivenessResult.Skipped
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
                    type,
                    CfirMatchPatternKind.Enum(enumType.classId, missingEntry, emptyList()),
                    null,
                )
            }
            ExhaustivenessResult.NonExhaustive(missing, source)
        }
    }

    companion object {
        val INSTANCE = SmallEnumBitVectorChecker()
    }
}
