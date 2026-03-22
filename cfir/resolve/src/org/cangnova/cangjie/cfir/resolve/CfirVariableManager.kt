package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag

class CfirVariableManager {
    private var nextFreshTypeId: Int = 1

    fun nextFreshTypeId(): Int = nextFreshTypeId++

    fun allocatePlaceholder(
        typeParameter: CfirTypeParameterSymbol,
        name: String,
    ): CfirTypeVariable = createTypeVariable(typeParameter, name)

    fun allocateInstantiationVariable(
        typeParameter: CfirTypeParameterSymbol,
        name: String,
    ): CfirTypeVariable = createTypeVariable(typeParameter, name)

    fun allocateDeferredBoundaryVariable(
        typeParameter: CfirTypeParameterSymbol,
        name: String,
    ): CfirTypeVariable = createTypeVariable(typeParameter, name)

    fun createTypeVariable(
        typeParameter: CfirTypeParameterSymbol,
        name: String,
    ): CfirTypeVariable {
        return CfirTypeVariable(
            typeParameter = typeParameter,
            freshTypeId = nextFreshTypeId(),
            lookupTag = ConeTypeParameterLookupTag(name),
        )
    }
}
