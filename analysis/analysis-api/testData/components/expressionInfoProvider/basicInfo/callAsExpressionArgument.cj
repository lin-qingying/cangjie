// FILE: callAsExpressionArgument.cj
// TARGET_EXPRESSION_TEXT: getValue()
// EXPECTED_IS_STATEMENT_LIKE: false
// EXPECTED_IS_COMPILE_TIME_CONSTANT: false
package sample.expressioninfo

func getValue(): Int64 {
    return 42
}

func consume(value: Int64) {
}

func useCall() {
    consume(getValue())
}
