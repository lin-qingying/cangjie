package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.types.ConeCangJieType

/** callable reference 适配结果，记录参数裁剪、默认值与函数种类转换。 */
internal class CallableReferenceAdaptation(
    /** 适配后的参数类型列表。 */
    val argumentTypes: List<ConeCangJieType> = emptyList(),
    /** 被默认值填充的参数数量。 */
    val defaults: Int,
    /** callable reference 函数种类转换策略。 */
    val conversionStrategy: CallableReferenceConversionStrategy = CallableReferenceConversionStrategy.NoConversion,
) {
    init {
        require(defaults >= 0) { "defaults must be non-negative" }
        require(defaults != 0 || hasFunctionKindConversion() || argumentTypes.isNotEmpty()) {
            "Adaptation must be non-trivial."
        }
    }

    /** 是否包含函数种类转换。 */
    fun hasFunctionKindConversion(): Boolean {
        return conversionStrategy != CallableReferenceConversionStrategy.NoConversion
    }
}

/** callable reference 适配时可使用的函数种类转换策略。 */
sealed class CallableReferenceConversionStrategy {
    /** 不执行函数种类转换。 */
    data object NoConversion : CallableReferenceConversionStrategy()

    /** 执行函数种类转换。 */
    data object FunctionKindConversion : CallableReferenceConversionStrategy()
}
