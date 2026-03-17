package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * Structured diagnostics carried by [ConeErrorType].
 *
 * Resolve/checkers can pattern-match these diagnostics instead of parsing strings.
 */
sealed interface ConeDiagnostic {
    val reason: String
}

/**
 * Base type for unresolved diagnostics.
 */
sealed interface ConeUnresolvedError : ConeDiagnostic

data class ConeUnresolvedReferenceError(
    val name: Name,
) : ConeUnresolvedError {
    override val reason: String = "unresolved reference: ${name.asString()}"
}

data class ConeUnresolvedSymbolError(
    val classId: ClassId,
) : ConeUnresolvedError {
    override val reason: String = "unresolved symbol: ${classId.asString()}"
}

data class ConeUnresolvedNameError(
    val name: Name,
    val operator: String? = null,
    val receiverType: ConeCangjieType? = null,
) : ConeUnresolvedError {
    override val reason: String = buildString {
        append("unresolved name: ")
        append(name.asString())
        if (operator != null) {
            append(", operator=")
            append(operator)
        }
        if (receiverType != null) {
            append(", receiver=")
            append(receiverType)
        }
    }
}
