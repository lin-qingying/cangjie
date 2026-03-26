package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.withReplacedSourceAndType
import org.cangnova.cangjie.source.CjSourceElement

fun CfirTypeRef.resolvedTypeFromPrototype(
    type: ConeCangJieType,
    fallbackSource: CjSourceElement?,
): CfirResolvedTypeRef {
    if (this is CfirResolvedTypeRef) {
        return withReplacedSourceAndType(source ?: fallbackSource, type)
    }
    return type.toCfirResolvedTypeRef(source ?: fallbackSource, this as? CfirUserTypeRef)
}