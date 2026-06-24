package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.name.ClassId

/**
 * 按声明 [ClassId] 查询直接父类型的 session 组件。
 *
 * 该接口返回的是声明上原始记录的 resolved type ref，尚未根据某个 use-site 的实际类型实参重写。
 */
interface CfirDirectSupertypeProvider : CfirSessionComponent {
    /**
     * 返回 [ownerClassId] 声明中直接写出的父类型列表。
     */
    fun getDirectSuperTypes(ownerClassId: ClassId): List<CfirResolvedTypeRef>
}
