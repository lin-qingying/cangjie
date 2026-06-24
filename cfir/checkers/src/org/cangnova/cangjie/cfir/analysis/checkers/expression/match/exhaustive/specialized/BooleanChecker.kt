package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.specialized

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirConstantValue
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

/** 针对布尔类型的二值覆盖穷尽性 checker。 */
class BooleanChecker : ExhaustivenessChecker {
    /** 当前 checker 在穷尽性诊断中使用的来源标记。 */
    override val source: CheckSource = CheckSource.BOOLEAN_FLAG

    /** 布尔专项 checker 的调度优先级。 */
    override val priority: Int = 10

    /** 仅在被检查类型为 primitive `BOOLEAN` 时启用。 */
    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: CheckerContext,
    ): Boolean = type is ConePrimitiveType && type.kind == PrimitiveTypeKind.BOOLEAN

    /** 检查模式矩阵首列是否同时覆盖 `true` 与 `false`。 */
    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
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

    /** 布尔穷尽性 checker 的共享单例容器。 */
    companion object {
        /** 默认布尔穷尽性 checker 单例。 */
        val INSTANCE = BooleanChecker()
    }
}
