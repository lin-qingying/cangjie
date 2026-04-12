// FILE: unresolvedReferenceElement.cj
// TARGET_ELEMENT_TEXT: missingCall
// EXPECTED_ELEMENT_DIAGNOSTIC: UNRESOLVED_REFERENCE
package sample.diagnostics

func main() {
    missingCall()
}
