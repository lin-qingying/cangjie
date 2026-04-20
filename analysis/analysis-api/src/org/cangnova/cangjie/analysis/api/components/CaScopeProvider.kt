package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile

interface CaScopeProvider : CaLifetimeOwner {
    fun CjFile.getFileScope(): CaScope

    fun getPackageScope(packageFqName: FqName): CaScope?

    val CaPackageSymbol.packageScope: CaScope

    val CaClassLikeSymbol.declaredMemberScope: CaScope

    val CaExtendSymbol.declaredMemberScope: CaScope

    val CaClassLikeSymbol.memberScope: CaScope

    val CaType.scope: CaScope?
}
