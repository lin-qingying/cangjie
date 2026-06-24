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
    /**
     * 返回 [type] 在当前 session 中可见的直接父类型。
     *
     * 返回值已经应用 [type] 的类型实参，并可包含 extend 语义补充的接口父类型。
     */
    fun getDirectSupertypes(type: ConeCangJieType): List<ConeCangJieType>
}
