package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * Composite source provider.
 */
class CfirCompositeProvider(
    private val providers: List<CfirProvider>,
) : CfirProvider {
    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> =
        providers.flatMap { it.getCfirFilesByPackage(fqName) }

    override fun getClassByClassId(classId: ClassId): CfirClass? {
        for (provider in providers) {
            val result = provider.getClassByClassId(classId)
            if (result != null) return result
        }
        return null
    }
}
