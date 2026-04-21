package org.cangnova.cangjie.cfir.resolve.match.exhaustive.specialized

import org.cangnova.cangjie.cfir.resolve.match.CfirConstantValue
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

class BooleanChecker : ExhaustivenessChecker {
    override val source: CheckSource = CheckSource.BOOLEAN_FLAG
    override val priority: Int = 10

    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): Boolean = type is ConePrimitiveType && type.kind == PrimitiveTypeKind.BOOLEAN

    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult {
        if (!isApplicable(type, emptyList(), context)) return ExhaustivenessResult.Skipped

        var hasTrue = false
        var hasFalse = false

        for (row in matrix) {
            val pattern = row.firstOrNull() ?: continue
            when (val kind = pattern.kind) {
                CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> {
                    hasTrue = true
                    hasFalse = true
                }

                is CfirMatchPatternKind.Const -> {
                    val value = kind.value as? CfirConstantValue.BooleanConst
                    if (value != null) {
                        if (value.value) hasTrue = true else hasFalse = true
                    }
                }

                else -> Unit
            }
            if (hasTrue && hasFalse) return ExhaustivenessResult.Exhaustive
        }

        return if (hasTrue && hasFalse) {
            ExhaustivenessResult.Exhaustive
        } else {
            val missing = mutableListOf<CfirMatchPattern>()
            if (!hasTrue) {
                missing += CfirMatchPattern(
                    type,
                    CfirMatchPatternKind.Const(CfirConstantValue.BooleanConst(true)),
                    null,
                )
            }
            if (!hasFalse) {
                missing += CfirMatchPattern(
                    type,
                    CfirMatchPatternKind.Const(CfirConstantValue.BooleanConst(false)),
                    null,
                )
            }
            ExhaustivenessResult.NonExhaustive(missing, source)
        }
    }

    companion object {
        val INSTANCE = BooleanChecker()
    }
}
