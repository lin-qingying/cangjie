// FILE: immutableReference.cj
// TARGET_EXPRESSION_TEXT: (stableAnswer)
// EXPECTED_STABILITY: COMPUTED_VALUE
// EXPECTED_IS_PURE_REFERENCE: true
package sample.dataflow

func useStableReference() {
    let stableAnswer: Int64 = 42
    let copy = (stableAnswer)
}
