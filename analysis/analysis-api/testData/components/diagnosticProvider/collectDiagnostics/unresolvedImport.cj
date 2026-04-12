// FILE: unresolvedImport.cj
// EXPECTED_COMMON_DIAGNOSTIC: UNRESOLVED_IMPORT
package sample.diagnostics

import ghost.pkg.MissingSymbol

func main() {
    let answer = 42
}
