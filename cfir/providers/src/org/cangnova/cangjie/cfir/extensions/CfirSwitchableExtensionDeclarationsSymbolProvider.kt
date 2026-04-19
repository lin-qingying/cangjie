package org.cangnova.cangjie.cfir.extensions

import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
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

    override val symbolNamesProvider: CfirSymbolNamesProvider = object : CfirSymbolNamesProvider() {
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

    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
        if (disabled) return
        delegate.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        if (disabled) return
        delegate.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        if (disabled) return
        delegate.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
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
