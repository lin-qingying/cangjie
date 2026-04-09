// FILE: callExpression.cj
// TARGET_EXPRESSION_TEXT: computeValue()
// EXPECTED_STABILITY: COMPUTED_VALUE
// EXPECTED_IS_PURE_REFERENCE: false
// EXPECTED_EXPRESSION_TYPE: Int64
package sample.dataflow

func computeValue(): Int64 {
    return 1
}

func useCall() {
    let value = computeValue()
}
