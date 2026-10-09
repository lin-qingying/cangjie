package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.hasOmittedLambdaParameterType
import org.cangnova.cangjie.cfir.declarations.lambdaParameterInferenceFailedAtDefinition
import org.cangnova.cangjie.cfir.resolve.calls.hasUninferredLambdaParameterType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * Lambda 参数推断失败的共享判定。
 *
 * 官方 `SynLamExpr` 会先把省略类型的参数作为 placeholder 参与 body 推断；
 * 若最终仍有任一省略参数无法解出，只在第一个省略参数上报告注解缺失，
 * body 中由同一 placeholder 派生的二级错误由各 checker 共享抑制。
 */
internal fun CfirAnonymousFunction.firstOmittedLambdaParameterForInferenceFailure(): CfirValueParameter? {
    if (!isLambda || !hasExplicitParameterList) return null
    val omittedParameters = valueParameters.filter { it.hasOmittedLambdaParameterType() }
    if (omittedParameters.isEmpty()) return null

    // 官方在 lambda 自身合成结束（SolveLamExprParamTys）时决定结论，之后出现的约束
    // （例如后续 `g(i)` 反解 placeholder）不改变结论；CFIR 的最终类型会被后续约束改写，
    // 所以优先消费 resolve 侧记录的定义点事实，事实缺失时才回退到实时类型判定。
    val inferenceFailed = lambdaParameterInferenceFailedAtDefinition
        ?: omittedParameters.any { it.hasUninferredLambdaParameterType() }
    return if (inferenceFailed) omittedParameters.first() else null
}

/**
 * 是否存在仍未解出的省略类型 lambda 参数。
 *
 * 该判定用于抑制由 placeholder 派生的二级错误，关注的是 body 检查时的最终类型状态，
 * 因此刻意只读实时类型（不消费定义点事实）。
 */
internal fun CfirAnonymousFunction.hasUninferredOmittedLambdaParameterType(): Boolean =
    valueParameters.any { it.hasOmittedLambdaParameterType() && it.hasUninferredLambdaParameterType() }

/**
 * 取得源码显式写出的 lambda 参数类型。
 *
 * 类型引用可能经过多次解析和替换，并把原型保留在 delegated 链中。形状诊断追溯到
 * 最初解析自源码的显式类型，不能把后续阶段的中间类型视图当作用户标注。
 */
internal fun CfirValueParameter.explicitLambdaParameterType(): ConeCangJieType? {
    if (hasOmittedLambdaParameterType()) return null

    var current = returnTypeRef
    var explicitType: ConeCangJieType? = null
    while (current is CfirResolvedTypeRef) {
        explicitType = current.coneType
        current = current.delegatedTypeRef ?: break
    }
    return explicitType
}
