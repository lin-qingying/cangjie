package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 源模块声明查询接口。
 *
 * 提供按包名和 ClassId 查询当前编译模块中声明的能力。
 * 参考 K2 FirProvider。
 */
interface CfirProvider : CfirSessionComponent {

    /** 获取指定包下的所有 CFIR 源文件 */
    fun getCfirFilesByPackage(fqName: FqName): List<CfirFile>

    /** 通过 ClassId 查找类声明 */
    fun getClassByClassId(classId: ClassId): CfirClass?
}
