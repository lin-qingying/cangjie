package org.cangnova.cangjie.cfir.resolve.match

import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.isBoolean
import org.cangnova.cangjie.cfir.types.isIntegerType
import org.cangnova.cangjie.cfir.types.isRune

/** 官方 TranslateOrPattern 为 OR 选择的控制流形态。 */
enum class CfirOrPatternLoweringKind {
    /** String、Float、tuple、type 等模式仍按 alternative 顺序判定。 */
    SEQUENTIAL,
    /** 可比较的离散值共享一个 MultiBranch 成功块。 */
    MULTI_BRANCH,
    /** wildcard 或 Unit 的 OR 不需要运行时判定。 */
    UNCONDITIONAL,
}

/** 选择共享 CFG/CFA 使用的 OR 形态，不把全部 OR 都展开为独立条件链。 */
fun CfirOrPattern.loweringKind(): CfirOrPatternLoweringKind {
    if (alternatives.isEmpty()) return CfirOrPatternLoweringKind.SEQUENTIAL
    if (alternatives.all { it is CfirWildcardPattern }) return CfirOrPatternLoweringKind.UNCONDITIONAL
    if (alternatives.all { it is CfirConstPattern }) {
        val types = alternatives.map { (it as CfirConstPattern).expression.coneTypeOrNull }
        if (types.all { it?.isUnit == true }) return CfirOrPatternLoweringKind.UNCONDITIONAL
        if (types.all { it != null && (it.isIntegerType || it.isBoolean || it.isRune) }) {
            return CfirOrPatternLoweringKind.MULTI_BRANCH
        }
    }
    if (alternatives.all { it is CfirEnumPattern && it.arguments.all { argument -> argument is CfirWildcardPattern } }) {
        return CfirOrPatternLoweringKind.MULTI_BRANCH
    }
    return CfirOrPatternLoweringKind.SEQUENTIAL
}
