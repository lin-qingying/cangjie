package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Source declaration provider aligned with Kotlin FIR provider abstractions.
 */
abstract class CfirProvider : CfirSessionComponent {
    abstract val symbolProvider: CfirSymbolProvider

    abstract fun getCfirFilesByPackage(fqName: FqName): List<CfirFile>

    abstract fun getClassByClassId(classId: ClassId): CfirClassLikeDeclaration?

    abstract fun getClassNamesInPackage(fqName: FqName): Set<Name>
    abstract fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile

    open fun getCfirClassifierContainerFile(symbol: CfirClassLikeSymbol<*>): CfirFile =
        getCfirClassifierContainerFile(symbol.classId)

    open fun getClassIdBySymbol(classSymbol: CfirClassSymbol): ClassId? = null

    open fun getEnumConstructorOwnerClassId(symbol: CfirEnumConstructorSymbol): ClassId? = null

    /**
     * provider 级的声明归属查询必须与 symbolProvider 保持一致，
     * 这样 resolve/checker/cone 映射无论走哪条链路，都拿到同一份 owner/file 元数据。
     */
    open fun getContainingFile(symbol: CfirBasedSymbol<*>): CfirFile? =
        symbolProvider.getContainingFile(symbol.unwrapForDeclarationMetadataLookup())

    open fun getContainingClassId(symbol: CfirCallableSymbol<*>): ClassId? =
        symbolProvider.getContainingClassId(symbol.unwrapCallableForDeclarationMetadataLookup())
}
