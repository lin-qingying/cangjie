package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.source.CjFakeSourceElementKind

/**
 * Lambda 形参是否省略了源码类型标注。
 *
 * 该信息属于 raw CFIR 的语法事实，不能在补全后通过当前类型反推；
 * lambda 参数推断失败时，checker 需要按源码中第一个省略参数定位诊断。
 */
private object LambdaParameterTypeOmittedKey : CfirDeclarationDataKey()

/**
 * lambda 形参是否省略了源码类型标注的原始语法事实。
 *
 * 取值语义：`true` / `false` 表示 raw 阶段已记录的省略与否；
 * `null` 表示该参数尚未被 raw 层标记（如编译器合成参数），
 * 读取方应回退到 [hasOmittedLambdaParameterType] 的推导逻辑。
 */
var CfirValueParameter.isLambdaParameterTypeOmitted: Boolean? by
    CfirDeclarationDataRegistry.data(LambdaParameterTypeOmittedKey)

/** 统一读取源码省略信息；编译器合成参数尚无raw标记时，使用其隐式type-ref/source种类。 */
fun CfirValueParameter.hasOmittedLambdaParameterType(): Boolean =
    isLambdaParameterTypeOmitted
        ?: (returnTypeRef is CfirImplicitTypeRef ||
                returnTypeRef.source?.kind == CjFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter)

/**
 * Lambda 头部诊断使用的目标函数类型。
 *
 * 调用完成阶段在错误候选上不会把类型强行写入 [CfirAnonymousFunction.matchingParameterFunctionType]，
 * 但 checker 仍需要官方 `ChkLamParamTys` 使用的目标函数形状来报告参数个数、
 * 显式参数类型和返回体类型错误。
 */
private object LambdaParameterShapeExpectedFunctionTypeKey : CfirDeclarationDataKey()

/**
 * lambda 头部诊断使用的目标函数形状。
 *
 * 仅在调用完成阶段由错误候选写入；正常完成时类型已落在
 * [CfirAnonymousFunction.matchingParameterFunctionType] 上，此键保持为 `null`。
 */
var CfirAnonymousFunction.lambdaParameterShapeExpectedFunctionType: ConeFunctionType? by
    CfirDeclarationDataRegistry.data(LambdaParameterShapeExpectedFunctionTypeKey)

/**
 * 当前 lambda 是否位于参数映射已经失败、因而不得继续做实参类型检查的调用参数中。
 *
 * 该状态由调用完成写回阶段从结构化 ArgumentMappingOutcome 向整个实参子树传播。
 * checker 只消费这一语义状态，不从具体诊断名称或错误文本反推调用是否已经终止。
 */
private object LambdaInsideFailedArgumentMappingKey : CfirDeclarationDataKey()

/**
 * 当前 lambda 是否位于参数映射已失败的调用实参子树中。
 *
 * 由调用完成写回阶段从结构化 ArgumentMappingOutcome 传播；为 `null`
 * 表示该 lambda 不在任何调用的实参位置，checker 可直接跳过相关检查。
 */
var CfirAnonymousFunction.isInsideFailedArgumentMapping: Boolean? by
    CfirDeclarationDataRegistry.data(LambdaInsideFailedArgumentMappingKey)

/** IfAvailable 分支方向，用于在 checker 阶段恢复结构化可用性上下文。 */
enum class CfirIfAvailableBranchKind {
    THEN,
    ELSE,
}

/**
 * IfAvailable lambda 的 source-independent 条件事实。
 *
 * 该数据随匿名函数声明携带，避免 APILevel checker 通过 source offset 或 sibling
 * branch presence 猜测当前分支；嵌套 lambda 按 declaration traversal 顺序自然叠加。
 */
data class CfirIfAvailableBranchContext(
    val kind: CfirIfAvailableBranchKind,
    val conditionName: String,
    val conditionValue: String,
)

/** [CfirAnonymousFunction.ifAvailableBranchContext] 扩展属性使用的声明数据键。 */
private object IfAvailableBranchContextKey : CfirDeclarationDataKey()

/**
 * IfAvailable lambda 携带的 source-independent 条件事实。
 *
 * 随匿名函数声明存储，供 APILevel checker 按条件名与取值判断分支合法性；
 * 嵌套 lambda 按声明遍历顺序自然叠加各自的事实。
 */
var CfirAnonymousFunction.ifAvailableBranchContext: CfirIfAvailableBranchContext? by
    CfirDeclarationDataRegistry.data(IfAvailableBranchContextKey)
