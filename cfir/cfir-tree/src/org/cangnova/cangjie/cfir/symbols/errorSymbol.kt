package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.declarations.CfirErrorNamedValue
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.impl.CfirErrorTypeRefImpl
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name

open class CfirErrorNamedValueSymbol(
    override val callableId: CallableId,
    val diagnostic: ConeDiagnostic
) : CfirNamedValueSymbol<CfirErrorNamedValue>(callableId), CfirErrorCallableSymbol<CfirErrorNamedValue> {
    override val name: Name
        get() = callableId.callableName

    override fun toString(): String =
        if (isBound) "CfirErrorNamedValueSymbol(${cfir.name})" else "CfirErrorNamedValueSymbol(unbound)"
}

class CfirErrorEnumConstructorSymbol(
    callableId: CallableId,
    diagnostic: ConeDiagnostic,
) : CfirErrorNamedValueSymbol(callableId, diagnostic)
