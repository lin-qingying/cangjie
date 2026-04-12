// FILE: unresolvedImportElement.cj
// TARGET_ELEMENT_TEXT: MissingSymbol
// EXPECTED_ELEMENT_DIAGNOSTIC: UNRESOLVED_IMPORT
package sample.diagnostics

import ghost.pkg.MissingSymbol

func main() {
    let answer = 42
}
