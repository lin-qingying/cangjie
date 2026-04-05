package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 面向具体 use-site 类型的父类型提供器。
 *
 * 和仅按 [org.cangnova.cangjie.name.ClassId] 查询的 [CfirDirectSupertypeProvider] 不同，
 * 这里返回的是“已经按当前具体类型实参完成实例化后的父类型”。
 * 该接口专供类型系统使用，用来统一 declared supertype 与 extend 引入接口的语义。
 */
interface CfirTypeAwareSupertypeProvider : CfirSessionComponent {
    fun getDirectSupertypes(type: ConeCangJieType): List<ConeCangJieType>
}
