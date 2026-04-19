

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLEmptyKotlinSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLKotlinSourceSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLKotlinSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleSpecificSymbolProviderAccess
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.LLContainingClassCalculator
import org.cangnova.cangjie.cfir.ThreadSafeMutableState
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.withCfirSymbolEntry
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

@ThreadSafeMutableState
internal class LLCfirProvider(
    val session: LLCfirSession,
    private val moduleComponents: LLCfirModuleResolveComponents,
    canContainKotlinPackage: Boolean,
    disregardSelfDeclarations: Boolean = false,
    declarationProviderFactory: (GlobalSearchScope) -> CangJieDeclarationProvider?,
) : CfirProvider() {
    override val symbolProvider: LLKotlinSymbolProvider =
        if (!disregardSelfDeclarations) {
            LLKotlinSourceSymbolProvider(session, moduleComponents, canContainKotlinPackage, declarationProviderFactory)
        } else {
            LLEmptyKotlinSymbolProvider(session)
        }

    override val isPhasedCfirAllowed: Boolean get() = true

    override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? =
        symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir

    /**
     * @param classLikeDeclaration The [CjClassLikeDeclaration] must be contained in the module associated with this [LLCfirProvider]. See
     *  [LLModuleSpecificSymbolProviderAccess] for details.
     */
    fun getCfirClassifierByDeclaration(classLikeDeclaration: CjClassLikeDeclaration): CfirClassLikeDeclaration? {
        val classId = classLikeDeclaration.getClassId() ?: return null

        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return symbolProvider.getClassLikeSymbolByPsi(classId, classLikeDeclaration)?.cfir
    }

    override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile {
        return getCfirClassifierContainerFileIfAny(fqName)
            ?: errorWithAttachment("Couldn't find container") {
                withEntry("classId", fqName.asString())
            }
    }

    override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? {
        return getCfirClassifierByFqName(fqName)?.let { moduleComponents.cache.getContainerCfirFile(it) }
    }

    override fun getCfirClassifierContainerFile(symbol: CfirClassLikeSymbol<*>): CfirFile {
        return getCfirClassifierContainerFileIfAny(symbol)
            ?: errorWithAttachment("Couldn't find container") {
                withCfirSymbolEntry("symbol", symbol)
            }
    }

    override fun getCfirClassifierContainerFileIfAny(symbol: CfirClassLikeSymbol<*>): CfirFile? {
        return moduleComponents.cache.getContainerCfirFile(symbol.cfir)
    }

    override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? {
        return moduleComponents.cache.getContainerCfirFile(symbol.cfir)
    }

    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> = error("Should not be called in CFIR IDE")

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> =
        symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(fqName)
            ?: errorWithAttachment("Cannot compute the set of class names in the given package") {
                withEntry("packageFqName", fqName.asString())
            }

    override fun getContainingClass(symbol: CfirBasedSymbol<*>): CfirClassLikeSymbol<*>? {
        val psiResult = LLContainingClassCalculator.getContainingClassSymbol(symbol)
        if (psiResult != null) {
            return psiResult
        }

        return super.getContainingClass(symbol)
    }
}
