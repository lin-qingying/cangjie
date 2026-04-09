// FILE: constantExpression.cj
// TARGET_EXPRESSION_TEXT: 42
// EXPECTED_STABILITY: STABLE_VALUE
// EXPECTED_IS_PURE_REFERENCE: false
// EXPECTED_COMPILE_TIME_VALUE: 42
// EXPECTED_EXPRESSION_TYPE: Int64
package sample.dataflow

func useConstant() {
    let answer = 42
}
