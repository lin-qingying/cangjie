package org.cangnova.cangjie.analysis.api.symbols.markers

import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.name.Name

/**
 * 具备稳定名称的公开符号能力接口。
 */
interface CaNamedSymbol : CaSymbol {
    val name: Name
}

/**
 * 可作为其他声明容器的公开符号能力接口。
 *
 * 典型实现包括 file、script、class、extend、property。
 */
interface CaDeclarationContainerSymbol : CaSymbol

/**
 * 持有类型参数的公开符号能力接口。
 */
interface CaTypeParameterOwnerSymbol : CaSymbol {
    val typeParameters: List<CaTypeParameterSymbol>
}

/**
 * 持有值参数的公开符号能力接口。
 */
interface CaValueParameterOwnerSymbol : CaSymbol {
    val valueParameters: List<CaValueParameterSymbol>
}
