package org.cangjie.cfir.resolve.providers

import org.cangjie.cfir.declarations.CfirExtend
import org.cangjie.cfir.session.CfirSessionComponent
import org.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 仓颉特有 extend 声明查询接口。
 *
 * 仓颉语言的 extend 机制允许为已有类型添加接口实现和成员，
 * 此接口提供 extend 声明的查询能力。
 */
interface CfirExtendProvider : CfirSessionComponent {

    /** 获取指定类（标准库类型）的所有 extend 声明 */
    fun getExtendsForClass(classId: ClassId): List<CfirExtend>

    /** 获取指定包下的所有 extend 声明 */
    fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend>

    /** 获取内建原始类型的 extend 声明（如 extend Int64 <: Comparable<Int64>） */
    fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend>
}
