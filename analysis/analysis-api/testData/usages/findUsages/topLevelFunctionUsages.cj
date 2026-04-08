// TARGET_KIND: TOP_LEVEL_FUNCTION
// TARGET_NAME: greet
// EXPECTED_USAGE_COUNT: 3
// EXPECTED_USAGE_SCOPE_KIND: ANY
package sample.findusages

func greet() {
}

func firstCaller() {
    greet()
}

func secondCaller() {
    greet()
    greet()
}
