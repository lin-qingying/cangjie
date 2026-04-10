// FILE: mutableReference.cj
// TARGET_EXPRESSION_TEXT: (mutableCounter)
// EXPECTED_STABILITY: COMPUTED_VALUE
// EXPECTED_IS_PURE_REFERENCE: true
package sample.dataflow

func useMutableReference() {
    var mutableCounter: Int64 = 0
    let copy = (mutableCounter)
}
