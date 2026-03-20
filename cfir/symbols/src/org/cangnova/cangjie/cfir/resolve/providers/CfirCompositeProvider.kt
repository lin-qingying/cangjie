package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Composite source provider.
 */
class CfirCompositeProvider(
    session: CfirSession,
    private val providers: List<CfirProvider>,
) : CfirProvider() {
    override val symbolProvider: CfirSymbolProvider =
        CfirCompositeSymbolProvider(session,providers.map { it.symbolProvider })

    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> =
        providers.flatMap { it.getCfirFilesByPackage(fqName) }

    override fun getClassByClassId(classId: ClassId): CfirClass? {
        for (provider in providers) {
            val result = provider.getClassByClassId(classId)
            if (result != null) return result
        }
        return null
    }

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> {
        return buildSet {
            for (provider in providers) {
                addAll(provider.getClassNamesInPackage(fqName))
            }
        }
    }

    override fun getClassIdBySymbol(classSymbol: CfirClassSymbol): ClassId? {
        for (provider in providers) {
            val classId = provider.getClassIdBySymbol(classSymbol)
            if (classId != null) return classId
        }
        return null
    }

    override fun getEnumConstructorOwnerClassId(symbol: CfirEnumConstructorSymbol): ClassId? {
        for (provider in providers) {
            val classId = provider.getEnumConstructorOwnerClassId(symbol)
            if (classId != null) return classId
        }
        return null
    }
}
