package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor

private object TypeAliasConstructorInfoKey : CfirDeclarationDataKey()

data class TypeAliasConstructorInfo<T : CfirFunction>(
    val originalConstructor: T,
    val typeAliasSymbol: CfirTypeAliasSymbol,
    val substitutor: ConeSubstitutor?,
)

var <T : CfirFunction> T.typeAliasConstructorInfo: TypeAliasConstructorInfo<T>? by CfirDeclarationDataRegistry.data(TypeAliasConstructorInfoKey)

val CfirConstructorSymbol.typeAliasConstructorInfo: TypeAliasConstructorInfo<*>?
    get() = cfir.typeAliasConstructorInfo
