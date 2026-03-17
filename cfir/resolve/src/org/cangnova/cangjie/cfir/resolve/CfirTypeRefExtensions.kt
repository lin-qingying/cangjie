package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.renderSemanticKey

internal fun CfirTypeRef.renderStableKey(): String = when (this) {
    is CfirResolvedTypeRef -> "resolved:${coneType.renderSemanticKey()}"
    is CfirUserTypeRef -> buildString {
        append("user:")
        append(qualifier.joinToString("."))
        if (typeArguments.isNotEmpty()) {
            append('<')
            append(typeArguments.joinToString(",") { it.renderStableKey() })
            append('>')
        }
    }
    is CfirBasicTypeRef -> "basic:${name.asString()}"
    is CfirImplicitTypeRef -> "implicit"
    is CfirErrorTypeRef -> "error:${reason}"
    else -> "${this::class.qualifiedName}:${toString()}"
}

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

