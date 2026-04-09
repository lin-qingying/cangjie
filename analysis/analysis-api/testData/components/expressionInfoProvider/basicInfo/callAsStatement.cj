// FILE: callAsStatement.cj
// TARGET_EXPRESSION_TEXT: logCall()
// EXPECTED_IS_STATEMENT_LIKE: true
// EXPECTED_IS_COMPILE_TIME_CONSTANT: false
package sample.expressioninfo

func logCall() {
}

func useCall() {
    logCall()
}
