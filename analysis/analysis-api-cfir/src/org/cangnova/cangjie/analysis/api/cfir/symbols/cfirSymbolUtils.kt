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

/**
 * 从 CFIR type-parameter owner 构造公开类型参数符号列表。
 */
internal fun <D> CfirBasedSymbol<D>.createCjTypeParameters(
    builder: CaSymbolByCfirBuilder
): List<CaTypeParameterSymbol> where D : CfirTypeParameterRefsOwner, D : CfirDeclaration {
    return cfir.typeParameters.map { typeParameter ->
        builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol)
    }
}

/**
 * 从 CFIR 函数符号构造公开值参数符号列表。
 */
internal fun CfirFunctionSymbol<*>.createCjValueParameters(builder: CaSymbolByCfirBuilder): List<CaValueParameterSymbol> {
    return cfir.valueParameters.map { valueParameter ->
        builder.variableBuilder.buildValueParameterSymbol(valueParameter.symbol)
    }
}

/**
 * 取得 CFIR callable 在公开 API 中使用的 callableId。
 */
internal fun CfirCallableSymbol<*>.getCallableId(): CallableId? {
    return when {

        else -> callableId
    }
}
