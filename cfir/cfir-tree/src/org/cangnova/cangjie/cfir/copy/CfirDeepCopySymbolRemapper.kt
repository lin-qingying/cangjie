package org.cangnova.cangjie.cfir.copy

import org.cangnova.cangjie.cfir.symbols.*

/** 与 Kotlin DeepCopySymbolRemapper 一样，声明定义先分配符号，再复制引用边。 */
internal fun freshCfirOwnedSymbol(symbol: CfirBasedSymbol<*>): CfirBasedSymbol<*> = when (symbol) {
    is CfirBuiltInTypeSymbol -> CfirBuiltInTypeSymbol(symbol.classId, symbol.kind)
    is CfirPrimitiveTypeSymbol -> CfirPrimitiveTypeSymbol(symbol.classId, symbol.kind)
    is CfirErrorEnumConstructorSymbol -> CfirErrorEnumConstructorSymbol(symbol.callableId, symbol.diagnostic)
    is CfirErrorNamedValueSymbol -> CfirErrorNamedValueSymbol(symbol.callableId, symbol.diagnostic)
    is CfirErrorFunctionSymbol -> CfirErrorFunctionSymbol()
    is CfirClassSymbol -> CfirClassSymbol(symbol.classId)
    is CfirInterfaceSymbol -> CfirInterfaceSymbol(symbol.classId)
    is CfirStructSymbol -> CfirStructSymbol(symbol.classId)
    is CfirEnumSymbol -> CfirEnumSymbol(symbol.classId)
    is CfirTypeAliasSymbol -> CfirTypeAliasSymbol(symbol.classId)
    is CfirTypeParameterSymbol -> CfirTypeParameterSymbol()
    is CfirNamedFunctionSymbol -> CfirNamedFunctionSymbol(symbol.callableId)
    is CfirAnonymousFunctionSymbol -> CfirAnonymousFunctionSymbol()
    is CfirMainFunctionSymbol -> CfirMainFunctionSymbol(symbol.callableId)
    is CfirMacroDeclarationSymbol -> CfirMacroDeclarationSymbol(symbol.callableId)
    is CfirFinalizerSymbol -> CfirFinalizerSymbol(symbol.callableId)
    is CfirConstructorSymbol -> CfirConstructorSymbol(symbol.callableId)
    is CfirPropertySymbol -> CfirPropertySymbol(symbol.callableId)
    is CfirPropertyAccessorSymbol -> CfirPropertyAccessorSymbol()
    is CfirFieldVariableSymbol -> CfirFieldVariableSymbol(symbol.callableId)
    is CfirResourceVariableSymbol -> CfirResourceVariableSymbol(symbol.callableId)
    is CfirPatternVariableSymbol -> CfirPatternVariableSymbol(symbol.callableId)
    is CfirPatternBindingSymbol -> CfirPatternBindingSymbol(symbol.callableId)
    is CfirValueParameterSymbol -> CfirValueParameterSymbol(symbol.callableId)
    is CfirFileSymbol -> CfirFileSymbol()
    is CfirCodeFragmentSymbol -> CfirCodeFragmentSymbol()
    is CfirExtendSymbol -> CfirExtendSymbol()
    is CfirEnumConstructorSymbol -> CfirEnumConstructorSymbol(symbol.callableId)
    is CfirInvalidDeclarationSymbol -> CfirInvalidDeclarationSymbol()
    is CfirMacroExpansionDeclarationSymbol -> CfirMacroExpansionDeclarationSymbol()
    else -> error("Symbol has no owned CFIR declaration kind: " + symbol)
}
