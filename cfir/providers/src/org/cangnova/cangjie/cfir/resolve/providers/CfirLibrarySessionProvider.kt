package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 仅承载 library symbol provider 的 provider 壳。
 *
 * 库端没有源码 container file 真相，因此 file 相关查询默认不可用。
 */
class CfirLibrarySessionProvider(
    override val symbolProvider: CfirSymbolProvider,
) : CfirProvider() {
    override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? =
        symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir

    override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile = shouldNotBeCalled()

    override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? = null

    override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? = null

    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> = emptyList()

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> = shouldNotBeCalled()

    private fun shouldNotBeCalled(): Nothing = error("Should not be called for CfirLibrarySessionProvider")
}
