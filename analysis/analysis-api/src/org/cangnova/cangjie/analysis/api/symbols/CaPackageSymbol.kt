package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 包的公开语义视图。
 */
interface CaPackageSymbol : CaSymbol, CaNamedSymbol {
    val fqName: FqName

    override val name: Name
        get() = fqName.shortNameOrSpecial()
}
