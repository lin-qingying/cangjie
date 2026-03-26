package org.cangnova.cangjie.cfir.types

/**
 * Structured diagnostics carried by [org.cangnova.cangjie.cfir.types.ConeErrorType].
 *
 * Resolve/checkers can pattern-match these diagnostics instead of parsing strings.
 */
interface ConeDiagnostic {
    val reason: String
    val readableDescriptionAsTypeConstructor: String get() = reason

}

/**
 * A [ConeDiagnostic] that is never reported.
 * It is used when multiple FIR nodes, like a type ref and a reference, would otherwise contain the same diagnostic,
 * which would lead to duplicate diagnostics being reported.
 *
 * Call sites should document which FIR element's diagnostic this is duplicating and why the usage won't lead to missed diagnostics.
 */
class ConeUnreportedDuplicateDiagnostic(val original: ConeDiagnostic) :
    ConeDiagnostic {
    override val reason: String get() = original.reason
}