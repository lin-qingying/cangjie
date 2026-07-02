package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.types.ConeFunctionType

/**
 * Lambda 形参是否省略了源码类型标注。
 *
 * 该信息属于 raw CFIR 的语法事实，不能在补全后通过当前类型反推；
 * lambda 参数推断失败时，checker 需要按源码中第一个省略参数定位诊断。
 */
private object LambdaParameterTypeOmittedKey : CfirDeclarationDataKey()

var CfirValueParameter.isLambdaParameterTypeOmitted: Boolean? by
    CfirDeclarationDataRegistry.data(LambdaParameterTypeOmittedKey)

/**
 * Lambda 头部已经由调用解析阶段报告过参数形状错误。
 *
 * 官方 `ChkLamExpr` 在参数个数或显式参数类型不兼容时，会把该错误作为
 * lambda 形状错误处理；后续 checker 不能再把同一根因降级成省略参数缺少注解。
 */
private object LambdaParameterShapeDiagnosticKey : CfirDeclarationDataKey()

var CfirAnonymousFunction.hasLambdaParameterShapeDiagnostic: Boolean? by
    CfirDeclarationDataRegistry.data(LambdaParameterShapeDiagnosticKey)

/**
 * Lambda 头部诊断使用的目标函数类型。
 *
 * 调用完成阶段在错误候选上不会把类型强行写入 [CfirAnonymousFunction.matchingParameterFunctionType]，
 * 但 checker 仍需要官方 `ChkLamParamTys` 使用的目标函数形状来报告参数个数、
 * 显式参数类型和返回体类型错误。
 */
private object LambdaParameterShapeExpectedFunctionTypeKey : CfirDeclarationDataKey()

var CfirAnonymousFunction.lambdaParameterShapeExpectedFunctionType: ConeFunctionType? by
    CfirDeclarationDataRegistry.data(LambdaParameterShapeExpectedFunctionTypeKey)
