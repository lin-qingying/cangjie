package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

val CfirExpression.hasResolvedType: Boolean get() = coneTypeOrNull != null


val CfirTypeRef.coneTypeOrNull: ConeCangJieType?
    get() = coneTypeSafe()

val CfirTypeRef.coneType: ConeCangJieType
    get() = coneTypeSafe()
        ?: errorWithAttachment("Expected ${CfirResolvedTypeRef::class.simpleName} with ${ConeCangJieType::class.simpleName} but was ${this::class.simpleName}") {
            withCfirEntry("typeRef", this@coneType)
        }
@OptIn(ExperimentalContracts::class)

inline fun <reified T : ConeCangJieType> CfirTypeRef.coneTypeSafe(): T? {
    contract {
        returnsNotNull() implies (this@coneTypeSafe is CfirResolvedTypeRef)
    }
    return (this as? CfirResolvedTypeRef)?.coneType as? T
}

val CfirExpression.resolvedType: ConeCangJieType
    get() = coneTypeOrNull
        ?: errorWithAttachment("Expected expression '${this::class.simpleName}' to be resolved") {
            withCfirEntry("expression", this@resolvedType)
        }
