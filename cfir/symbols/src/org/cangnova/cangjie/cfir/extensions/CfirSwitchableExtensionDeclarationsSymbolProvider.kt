package org.cangnova.cangjie.cfir.extensions

import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

open class CfirSwitchableExtensionDeclarationsSymbolProvider protected constructor(
    private val delegate: CfirExtensionDeclarationsSymbolProvider,
) : CfirSymbolProvider(delegate.session) {
    companion object {
        fun createIfNeeded(session: CfirSession): CfirSwitchableExtensionDeclarationsSymbolProvider? =
            CfirExtensionDeclarationsSymbolProvider.createIfNeeded(session)?.let(::CfirSwitchableExtensionDeclarationsSymbolProvider)
    }

    protected open var disabled: Boolean = false

    override val symbolNamesProvider: CfirSymbolNamesProvider = object : CfirSymbolNamesProvider {
        override fun getPackageNames(): Set<FqName>? =
            if (disabled) null else delegate.symbolNamesProvider.getPackageNames()

        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
            if (disabled) null else delegate.symbolNamesProvider.getTopLevelClassifierNamesInPackage(packageFqName)

        override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
            if (disabled) null else delegate.symbolNamesProvider.getTopLevelCallableNamesInPackage(packageFqName)
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId):  CfirClassLikeSymbol<*>? {
        if (disabled) return null
        return delegate.getClassLikeSymbolByClassId(classId)
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> {
        if (disabled) return emptyList()
        return delegate.getTopLevelCallableSymbols(packageFqName, name)
    }

    override fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<CfirFunctionSymbol<*>> {
        if (disabled) return emptyList()
        return delegate.getTopLevelFunctionSymbols(packageFqName, name)
    }

    override fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<CfirPropertySymbol> {
        if (disabled) return emptyList()
        return delegate.getTopLevelPropertySymbols(packageFqName, name)
    }

    override fun hasPackage(fqName: FqName): Boolean {
        if (disabled) return false
        return delegate.hasPackage(fqName)
    }

    fun disable() {
        require(!disabled) {
            "Attempt to disable already disabled ${CfirSwitchableExtensionDeclarationsSymbolProvider::class}"
        }
        disabled = true
    }

    fun enable() {
        require(disabled) {
            "Attempt to enable already enabled ${CfirSwitchableExtensionDeclarationsSymbolProvider::class}"
        }
        disabled = false
    }

    internal fun isDisabled(): Boolean = disabled
}

val CfirSession.generatedDeclarationsSymbolProvider: CfirSwitchableExtensionDeclarationsSymbolProvider?
    by CfirSession.nullableSessionComponentAccessor()

fun CfirSession.withGeneratedDeclarationsSymbolProviderDisabled(action: () -> Unit) {
    val enabledProvider = generatedDeclarationsSymbolProvider?.takeUnless { it.isDisabled() }
    enabledProvider?.disable()
    try {
        action()
    } finally {
        enabledProvider?.enable()
    }
}
