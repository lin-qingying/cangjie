// FILE: compileTimeConstantLiteral.cj
// TARGET_EXPRESSION_TEXT: 42
// EXPECTED_IS_STATEMENT_LIKE: false
// EXPECTED_IS_COMPILE_TIME_CONSTANT: true
package sample.expressioninfo

func useLiteral() {
    let value = 42
}
