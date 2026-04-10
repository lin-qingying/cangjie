// FILE: plainStringLiteral.cj
// TARGET_EXPRESSION_TEXT: "hello"
// EXPECTED_IS_STATEMENT_LIKE: false
// EXPECTED_IS_COMPILE_TIME_CONSTANT: true
package sample.expressioninfo

func useString() {
    let text = "hello"
}
