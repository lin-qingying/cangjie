package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile

interface CaScopeProvider : CaLifetimeOwner {
    fun CjFile.getFileScope(): CaScope

    fun getPackageScope(packageFqName: FqName): CaScope?

    val CaPackageSymbol.packageScope: CaScope
    /**
     * A [KaScope] containing *all* members explicitly declared in the given [KaDeclarationContainerSymbol].
     *
     * In contrast to [declaredMemberScope] and [staticDeclaredMemberScope], this scope contains both static and non-static members.
     */
    public val CaDeclarationContainerSymbol.combinedDeclaredMemberScope: CaScope

    val CaClassLikeSymbol.declaredMemberScope: CaScope

    val CaExtendSymbol.declaredMemberScope: CaScope

    val CaClassLikeSymbol.memberScope: CaScope
    public val CaDeclarationContainerSymbol.memberScope: CaScope

    val CaType.scope: CaScope?
}

/**
 * Auto-generated bridge. DO NOT EDIT MANUALLY!
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
public fun CjFile.getFileScope(): CaScope {
    return with(session) {
        getFileScope()
    }
}

/**
 * Auto-generated bridge. DO NOT EDIT MANUALLY!
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
public fun getPackageScope(packageFqName: FqName): CaScope? {
    return with(session) {
        getPackageScope(packageFqName)
    }
}

/**
 * Auto-generated bridge. DO NOT EDIT MANUALLY!
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
public val CaPackageSymbol.packageScope: CaScope
    get() = with(session) { packageScope }

/**
 * Auto-generated bridge. DO NOT EDIT MANUALLY!
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
public val CaDeclarationContainerSymbol.combinedDeclaredMemberScope: CaScope
    get() = with(session) { combinedDeclaredMemberScope }

/**
 * Auto-generated bridge. DO NOT EDIT MANUALLY!
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
public val CaClassLikeSymbol.declaredMemberScope: CaScope
    get() = with(session) { declaredMemberScope }

/**
 * Auto-generated bridge. DO NOT EDIT MANUALLY!
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
public val CaExtendSymbol.declaredMemberScope: CaScope
    get() = with(session) { declaredMemberScope }

/**
 * Auto-generated bridge. DO NOT EDIT MANUALLY!
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
public val CaClassLikeSymbol.memberScope: CaScope
    get() = with(session) { memberScope }

/**
 * Auto-generated bridge. DO NOT EDIT MANUALLY!
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
public val CaDeclarationContainerSymbol.memberScope: CaScope
    get() = with(session) { memberScope }

/**
 * Auto-generated bridge. DO NOT EDIT MANUALLY!
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
public val CaType.scope: CaScope?
    get() = with(session) { scope }
