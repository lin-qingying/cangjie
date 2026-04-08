// FILE: localBinding.cj
// TARGET_NAME: localValue
// VISIBILITY_TARGET_KIND: BINDING_PATTERN
// EXPECTED_SYMBOL_VISIBILITY: LOCAL
// EXPECTED_VISIBILITY_EXPLICIT: false
// EXPECTED_IS_VISIBLE: true
package sample.visibility

func consume(): Int64 {
    let localValue = 1
    return localValue
}
