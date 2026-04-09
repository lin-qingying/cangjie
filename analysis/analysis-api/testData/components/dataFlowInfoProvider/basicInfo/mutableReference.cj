// FILE: mutableReference.cj
// TARGET_EXPRESSION_TEXT: mutableCounter
// EXPECTED_STABILITY: MUTABLE_VALUE
// EXPECTED_IS_PURE_REFERENCE: true
// EXPECTED_EXPRESSION_TYPE: Int64
package sample.dataflow

func useMutableReference() {
    var mutableCounter: Int64 = 0
    let copy = mutableCounter
}
