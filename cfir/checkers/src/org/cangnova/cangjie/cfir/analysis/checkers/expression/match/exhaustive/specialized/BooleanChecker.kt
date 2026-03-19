package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.specialized

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirConstantValue
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

class BooleanChecker : ExhaustivenessChecker {
    override val source: CheckSource = CheckSource.BOOLEAN_FLAG
    override val priority: Int = 10

    override fun isApplicable(
        type: ConeCangjieType,
        patterns: List<CfirMatchPattern>,
        context: CheckerContext,
    ): Boolean = type is ConePrimitiveType && type.kind == PrimitiveTypeKind.BOOLEAN

    override fun check(
        matrix: CfirMatrix,
        type: ConeCangjieType,
        context: CheckerContext,
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

