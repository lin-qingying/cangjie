package org.cangnova.cangjie.analysis.api.components

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
