package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 对齐 Kotlin FIR 的 composite provider。
 *
 * symbol lookup 由 [CfirCompositeSymbolProvider] 聚合，ownership/container 只在 provider 层聚合。
 */
class CfirCompositeProvider(
    session: CfirSession,
    private val providers: List<CfirProvider>,
) : CfirProvider() {
    override val symbolProvider: CfirSymbolProvider =
        CfirCompositeSymbolProvider(session, providers.map { it.symbolProvider })

    override val isPhasedCfirAllowed: Boolean
        get() = providers.any(CfirProvider::isPhasedCfirAllowed)

    override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? {
        for (provider in providers) {
            provider.getCfirClassifierByFqName(classId)?.let { return it }
        }
        return null
    }

    override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile =
        getCfirClassifierContainerFileIfAny(fqName)
            ?: error("No containing file found for classifier $fqName")

    override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? {
        for (provider in providers) {
            provider.getCfirClassifierContainerFileIfAny(fqName)?.let { return it }
        }
        return null
    }

    override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? {
        for (provider in providers) {
            provider.getCfirCallableContainerFile(symbol)?.let { return it }
        }
        return null
    }

    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> =
        providers.flatMap { it.getCfirFilesByPackage(fqName) }

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> {
        return buildSet {
            for (provider in providers) {
                addAll(provider.getClassNamesInPackage(fqName))
            }
        }
    }

    override fun getContainingClass(symbol: CfirBasedSymbol<*>): CfirClassLikeSymbol<*>? {
        for (provider in providers) {
            provider.getContainingClass(symbol)?.let { return it }
        }
        return null
    }
}
