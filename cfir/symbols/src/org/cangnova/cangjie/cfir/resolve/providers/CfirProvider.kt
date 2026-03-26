package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
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

    open fun getContainingFile(symbol: CfirSymbol<*>): CfirFile? = null

    open fun getContainingClassId(symbol: CfirCallableSymbol<*>): ClassId? = null
}
