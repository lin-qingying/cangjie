// FILE: multipleDiagnostics.cj
// EXPECTED_COMMON_DIAGNOSTIC: UNRESOLVED_IMPORT
// EXPECTED_COMMON_DIAGNOSTIC: UNRESOLVED_REFERENCE
package sample.diagnostics

import ghost.pkg.MissingSymbol

func main() {
    missingCall()
}
