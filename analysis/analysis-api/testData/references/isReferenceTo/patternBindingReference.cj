// FILE: patternBindingReference.cj
// TARGET_KIND: BINDING_PATTERN
// TARGET_NAME: value
// EXPECTED_TARGET_CLASS: CjBindingPattern
package sample.references.behavior

func consume(): Int64 {
    let value = 1
    return value
}
