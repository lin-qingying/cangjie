package org.cangnova.cangjie.cfir.analysis.diagnostics

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory2
import org.cangnova.cangjie.cfir.diagnostics.InternalDiagnosticFactoryMethod
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.types.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.types.ConeUnresolvedSymbolError

/**
 * Central bridge from structured cone diagnostics to CFIR frontend diagnostics.
 *
 * `callOrAssignmentSource` is kept for Kotlin FIR alignment and future richer mappings.
 */
fun ConeDiagnostic.toCfirDiagnostics(
    source: AbstractCjSourceElement?,
    context: CheckerContext,
    callOrAssignmentSource: AbstractCjSourceElement? = null,
): List<CjDiagnostic> {
    source ?: return emptyList()
    return listOfNotNull(mapToCfirDiagnostic(source, context, callOrAssignmentSource))
}

private fun ConeDiagnostic.mapToCfirDiagnostic(
    source: AbstractCjSourceElement,
    context: CheckerContext,
    callOrAssignmentSource: AbstractCjSourceElement?,
): CjDiagnostic? {
    // The parameter is intentionally threaded through even if current mappings do not consume it,
    // so collectors can keep Kotlin-style call-site context plumbing.
    callOrAssignmentSource

    return when (this) {
        is ConeUnresolvedReferenceError ->
            CfirErrors.UNRESOLVED_REFERENCE.on(source, name.asString(), null, context)

        is ConeUnresolvedSymbolError ->
            CfirErrors.UNRESOLVED_REFERENCE.on(source, classId.asString(), null, context)

        is ConeUnresolvedNameError ->
            CfirErrors.UNRESOLVED_REFERENCE.on(source, name.asString(), operator, context)
    }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun CjDiagnosticFactory2<String, String?>.on(
    source: AbstractCjSourceElement,
    a: String,
    b: String?,
    context: CheckerContext,
): CjDiagnostic? = on(source, a, b, null, context)

