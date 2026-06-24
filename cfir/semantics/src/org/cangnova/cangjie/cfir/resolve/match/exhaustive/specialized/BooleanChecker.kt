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

/**
 * Bool 类型专用穷尽性检查器。
 *
 * 只需要跟踪 `true` 与 `false` 两个常量是否被覆盖。
 */
class BooleanChecker : ExhaustivenessChecker {
    /** 当前 checker 来源。 */
    override val source: CheckSource = CheckSource.BOOLEAN_FLAG

    /** 当前 checker 优先级。 */
    override val priority: Int = 10

    /** Bool primitive 类型适用该 checker。 */
    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): Boolean = type is ConePrimitiveType && type.kind == PrimitiveTypeKind.BOOLEAN

    /** 检查 Bool 模式是否同时覆盖 true 和 false。 */
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

    /** 单例实例。 */
    companion object {
        /** 默认 Bool checker 实例。 */
        val INSTANCE = BooleanChecker()
    }
}
