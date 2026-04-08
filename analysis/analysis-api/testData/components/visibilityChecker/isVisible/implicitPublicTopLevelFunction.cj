// FILE: implicitPublicTopLevelFunction.cj
// TARGET_NAME: exportedGreeting
// VISIBILITY_TARGET_KIND: TOP_LEVEL_FUNCTION
// EXPECTED_SYMBOL_VISIBILITY: INTERNAL
// EXPECTED_VISIBILITY_EXPLICIT: false
// EXPECTED_IS_VISIBLE: true
package sample.visibility

func exportedGreeting(): Int64 {
    return 0
}

func consume(): Int64 {
    return exportedGreeting()
}
