package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name

/**
 * enum constructor 的 payload 形参工具集。
 *
 * `valueParameters` 是 payload 的唯一真相；
 * `returnTypeRef` 只表达“该构造器最终产出的枚举实例类型”。
 * 这样调用解析、模式匹配和声明渲染都可以共享一套一致的结构模型。
 */
fun enumConstructorPayloadParameterName(index: Int): Name = Name.identifier("enumCtorArg$index")

fun CfirEnumConstructor.payloadArity(): Int = valueParameters.size

fun CfirEnumConstructor.payloadParameterTypesOrEmpty(): List<ConeCangJieType> {
    val payloadTypes = ArrayList<ConeCangJieType>(valueParameters.size)
    for (valueParameter in valueParameters) {
        val payloadType = valueParameter.returnTypeRef.coneTypeOrNull ?: return emptyList()
        payloadTypes += payloadType
    }
    return payloadTypes
}
