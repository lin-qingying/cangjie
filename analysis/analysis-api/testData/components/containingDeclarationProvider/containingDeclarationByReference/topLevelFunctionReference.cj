// FILE: topLevelFunctionReference.cj
// TARGET_NAME: greet
// EXPECTED_CONTAINING_DECLARATION: sample.containing.reference.greet
package sample.containing.reference

func greet(): Int64 {
    return 0
}

func consume(): Int64 {
    return greet()
}
