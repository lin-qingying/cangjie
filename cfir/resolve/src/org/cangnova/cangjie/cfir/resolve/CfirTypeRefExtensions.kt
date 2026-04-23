package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.render.ConeTypeRendererForDebugInfo
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirOptionTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType

internal fun CfirTypeRef.renderStableKey(): String = when (this) {
    is CfirResolvedTypeRef -> "resolved:${coneType.renderStableSemanticKey()}"
    is CfirUserTypeRef -> buildString {
        append("user:")
        append(
            qualifier.joinToString(".") { qualifierPart ->
                buildString {
                    append(qualifierPart.name.asString())
                    if (qualifierPart.typeArguments.isNotEmpty()) {
                        append('<')
                        append(qualifierPart.typeArguments.joinToString(",") { it.renderStableKey() })
                        append('>')
                    }
                }
            }
        )
    }
    is CfirOptionTypeRef -> "option:${componentTypeRef.renderStableKey()}"
    is CfirBasicTypeRef -> "basic:${name.asString()}"
    is CfirImplicitTypeRef -> "implicit"
    is CfirErrorTypeRef -> "error:${diagnostic.reason}"
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

internal fun ConeCangJieType.renderStableSemanticKey(): String =
    buildString { ConeTypeRendererForDebugInfo(this).render(this@renderStableSemanticKey) }
