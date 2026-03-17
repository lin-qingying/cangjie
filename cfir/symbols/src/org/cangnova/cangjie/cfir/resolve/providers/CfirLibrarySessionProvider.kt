package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

class CfirLibrarySessionProvider(
    override val symbolProvider: CfirSymbolProvider
) : CfirProvider() {


    override fun getClassNamesInPackage(fqName: FqName): Set<Name> = shouldNotBeCalled()

    private fun shouldNotBeCalled(): Nothing = error("Should not be called for CfirLibrarySessionProvider")
    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> {
    return emptyList()

    }

    override fun getClassByClassId(classId: ClassId): CfirClass? {
        val symbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        return symbol.cfir as? CfirClass
    }


}
