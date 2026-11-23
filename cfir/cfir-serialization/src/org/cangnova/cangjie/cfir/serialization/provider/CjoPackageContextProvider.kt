package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode

/**
 * 按 CJO 包头和实际 binary owner 取得目标模块的正式上下文。
 * 包名仅作为元数据资源键；模块身份由目标 provider 的来源路径和 ModuleDataProvider 决定。
 * 此接口不暴露 session 的 PSI symbol provider，FullId 在返回的上下文中继续按 index/exportId 取 record。
 */
fun interface CjoPackageContextProvider {
    fun getPackageContext(packageName: String, mode: CjoDeserializationMode): CfirDeserializationContext?
}
