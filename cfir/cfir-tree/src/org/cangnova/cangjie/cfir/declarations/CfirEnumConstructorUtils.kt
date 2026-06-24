package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
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
/**
 * 返回 enum constructor payload 参数的合成名称。
 *
 * 该名称只用于 CFIR 内部建模，不表示源码中真实出现的参数名。
 */
fun enumConstructorPayloadParameterName(index: Int): Name = Name.identifier("enumCtorArg$index")

/**
 * 返回 enum constructor payload 参数个数。
 */
fun CfirEnumConstructor.payloadArity(): Int = valueParameters.size

/**
 * 返回 enum constructor payload 参数类型列表。
 *
 * 如果任一参数尚未形成可用 cone type，则返回空列表，表示该构造器 payload 类型当前不可消费。
 */
fun CfirEnumConstructor.payloadParameterTypesOrEmpty(): List<ConeCangJieType> {
    /**
     * enum constructor 的 payload 类型属于声明头信息。
     *
     * analysis / pattern resolve / renderer 都会通过这个共享入口读取 payload 参数类型，
     * 因此必须先把声明推进到 TYPES，不能要求所有调用方各自手动补 lazy resolve。
     */
    lazyResolveToPhase(CfirResolvePhase.TYPES)

    val payloadTypes = ArrayList<ConeCangJieType>(valueParameters.size)
    for (valueParameter in valueParameters) {
        val payloadType = valueParameter.returnTypeRef.coneTypeOrNull ?: return emptyList()
        payloadTypes += payloadType
    }
    return payloadTypes
}
