// FILE: callAsInitializer.cj
// TARGET_EXPRESSION_TEXT: buildValue()
// EXPECTED_IS_STATEMENT_LIKE: false
// EXPECTED_IS_COMPILE_TIME_CONSTANT: false
package sample.expressioninfo

func buildValue(): Int64 {
    return 1
}

func useCall() {
    let value = buildValue()
}
