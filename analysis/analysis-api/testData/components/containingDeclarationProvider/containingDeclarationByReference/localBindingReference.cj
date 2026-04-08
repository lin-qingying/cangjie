// FILE: localBindingReference.cj
// TARGET_NAME: value
// EXPECTED_CONTAINING_DECLARATION: value
// EXPECTED_CONTAINING_DECLARATION: sample.containing.reference.consume
package sample.containing.reference

func consume(): Int64 {
    let value = 1
    return value
}
