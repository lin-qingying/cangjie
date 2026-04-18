package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.name.CallableId

internal fun <D> CfirBasedSymbol<D>.createCjTypeParameters(
    builder: CaSymbolByCfirBuilder
): List<CaTypeParameterSymbol> where D : CfirTypeParameterRefsOwner, D : CfirDeclaration {
    return cfir.typeParameters.map { typeParameter ->
        builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol)
    }
}

internal fun CfirFunctionSymbol<*>.createCjValueParameters(builder: CaSymbolByCfirBuilder): List<CaValueParameterSymbol> {
    return cfir.valueParameters.map { valueParameter ->
        builder.variableBuilder.buildValueParameterSymbol(valueParameter.symbol)
    }
}

internal fun CfirCallableSymbol<*>.getCallableId(): CallableId? {
    return when {

        else -> callableId
    }
}
