package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef

internal fun CfirTypeRef.renderStableKey(): String = toString()

internal fun CfirTypeRef.isDefinitelyNotInterfaceType(): Boolean = when (this) {
    is CfirBasicTypeRef,
    is CfirImplicitTypeRef,
    is CfirErrorTypeRef -> true
    else -> false
}

internal fun CfirTypeRef.isDefinitelyIllegalExtendedType(): Boolean = when (this) {
    is CfirImplicitTypeRef,
    is CfirErrorTypeRef -> true
    else -> false
}
