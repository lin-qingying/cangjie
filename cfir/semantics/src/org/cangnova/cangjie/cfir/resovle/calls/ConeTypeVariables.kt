package org.cangnova.cangjie.cfir.resovle.calls

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
import org.cangnova.cangjie.name.SpecialNames

class ConeTypeVariableForPostponedAtom(name: String) : ConeTypeVariable(name)
class ConeTypeVariableForLambdaParameterType(name: String) : ConeTypeVariable(name)
class ConeTypeVariableForLambdaReturnType(val argument: CfirDeclaration, name: String) : ConeTypeVariable(name)

class ConeTypeParameterBasedTypeVariable(
    val typeParameterSymbol: CfirTypeParameterSymbol
) : ConeTypeVariable(SpecialNames.safeIdentifier(typeParameterSymbol.name).identifier, typeParameterSymbol.toLookupTag())
