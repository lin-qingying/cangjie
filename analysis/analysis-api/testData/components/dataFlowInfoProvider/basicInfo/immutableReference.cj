// FILE: immutableReference.cj
// TARGET_EXPRESSION_TEXT: stableAnswer
// EXPECTED_STABILITY: STABLE_VALUE
// EXPECTED_IS_PURE_REFERENCE: true
// EXPECTED_EXPRESSION_TYPE: Int64
package sample.dataflow

func useStableReference() {
    let stableAnswer: Int64 = 42
    let copy = stableAnswer
}
