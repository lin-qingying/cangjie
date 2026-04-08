package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * 源文件的公开语义视图。
 */
interface CaFileSymbol : CaSymbol, CaDeclarationContainerSymbol, CaNamedSymbol {
    val file: CjFile

    val packageFqName: FqName

    override val name: Name
        get() = Name.identifier(file.name)
}
