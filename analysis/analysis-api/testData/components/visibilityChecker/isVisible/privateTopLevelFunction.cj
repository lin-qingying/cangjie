// FILE: privateTopLevelFunction.cj
// TARGET_NAME: hiddenGreeting
// VISIBILITY_TARGET_KIND: TOP_LEVEL_FUNCTION
// EXPECTED_SYMBOL_VISIBILITY: PRIVATE
// EXPECTED_VISIBILITY_EXPLICIT: true
// EXPECTED_IS_VISIBLE: true
package sample.visibility

private func hiddenGreeting(): Int64 {
    return 0
}

func consume(): Int64 {
    return hiddenGreeting()
}
