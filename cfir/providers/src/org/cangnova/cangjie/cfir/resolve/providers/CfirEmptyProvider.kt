package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 空 provider，对齐 Kotlin FIR 的 empty provider。
 */
class CfirEmptyProvider(
    override val symbolProvider: CfirSymbolProvider,
) : CfirProvider() {
    override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? = null

    override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile =
        error("No containing file in CfirEmptyProvider")

    override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? = null

    override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? = null

    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> = emptyList()

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> = emptySet()
}
