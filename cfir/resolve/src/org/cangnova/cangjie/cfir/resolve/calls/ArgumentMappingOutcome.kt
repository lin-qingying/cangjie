package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 调用点在参数映射前即可确定的结构形状。
 *
 * 该对象由参数映射阶段唯一构造，overload 失败规约只消费结果，不再重新解析
 * PSI、source text 或复制调用节点探测参数形状。
 */
data class CallShape(
    /** 调用点实际提供的全部实参数量，包含尾随 closure。 */
    val actualArgumentCount: Int,
    /** 显式命名实参数量。 */
    val namedArgumentCount: Int,
    /** 尾随 closure 实参数量。 */
    val trailingLambdaCount: Int,
    /** 参数数量诊断的精确源码区间。 */
    val arityDiagnosticSource: AbstractCjSourceElement,
)

/**
 * 单个候选完成参数映射后的结构化结果。
 *
 * 该结果同时服务候选诊断和全部候选失败时的解释力排序，避免 resolver 从
 * `NoValueForParameter` / `TooManyArguments` 等具体诊断反推调用形状。
 */
data class ArgumentMappingOutcome(
    /** 调用点形状。 */
    val callShape: CallShape,
    /** 候选声明的完整形参数量，包含带默认值的形参。 */
    val expectedParameterCount: Int,
    /** 候选最少需要的实参数量。 */
    val requiredParameterCount: Int,
    /** 候选最多接受的实参数量；变参候选为空。 */
    val maximumAcceptedArgumentCount: Int?,
    /** 已成功绑定到形参的实参数量。 */
    val mappedArgumentCount: Int,
    /** 已成功按名称匹配的命名实参数量。 */
    val matchedNamedArgumentCount: Int,
    /** 本次映射是否产生阻断式调用形状错误。 */
    val hasMappingFailure: Boolean,
) {
    /** 实参数量到候选可接受区间的距离。 */
    val arityDistance: Int
        get() = when {
            callShape.actualArgumentCount < requiredParameterCount ->
                requiredParameterCount - callShape.actualArgumentCount

            maximumAcceptedArgumentCount != null &&
                    callShape.actualArgumentCount > maximumAcceptedArgumentCount ->
                callShape.actualArgumentCount - maximumAcceptedArgumentCount

            else -> 0
        }
}
