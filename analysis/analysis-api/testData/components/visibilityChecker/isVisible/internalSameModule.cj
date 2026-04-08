// FILE: internalSameModule.cj
// TARGET_NAME: internalGreeting
// VISIBILITY_TARGET_KIND: TOP_LEVEL_FUNCTION
// EXPECTED_SYMBOL_VISIBILITY: INTERNAL
// EXPECTED_VISIBILITY_EXPLICIT: true
// EXPECTED_IS_VISIBLE: true
package sample.visibility

internal func internalGreeting(): Int64 {
    return 0
}

func consume(): Int64 {
    return internalGreeting()
}
