package org.cangjie.cfir.resolve.providers

import org.cangjie.cfir.declarations.CfirClass
import org.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 最小可用的源声明 provider（K2 对齐：空实现可跑通解析链路）。
 */
class CfirEmptyProvider : CfirProvider {
    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> = emptyList()

    override fun getClassByClassId(classId: ClassId): CfirClass? = null
}

