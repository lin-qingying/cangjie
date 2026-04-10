// FILE: compileTimeTuple.cj
// TARGET_EXPRESSION_TEXT: (1, 2)
// EXPECTED_IS_STATEMENT_LIKE: false
// EXPECTED_IS_COMPILE_TIME_CONSTANT: true
package sample.expressioninfo

func useTuple() {
    let pair = (1, 2)
}
