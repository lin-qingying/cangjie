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