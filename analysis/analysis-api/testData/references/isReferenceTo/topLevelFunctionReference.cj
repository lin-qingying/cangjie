// FILE: topLevelFunctionReference.cj
// TARGET_KIND: TOP_LEVEL_FUNCTION
// TARGET_NAME: greet
// EXPECTED_TARGET_CLASS: CjNamedFunction
package sample.references.behavior

func greet(): Int64 {
    return 0
}

func consume(): Int64 {
    return greet()
}
