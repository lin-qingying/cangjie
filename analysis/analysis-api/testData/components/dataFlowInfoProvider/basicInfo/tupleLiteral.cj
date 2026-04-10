// FILE: tupleLiteral.cj
// TARGET_EXPRESSION_TEXT: (1, 2)
// EXPECTED_STABILITY: STABLE_VALUE
// EXPECTED_IS_PURE_REFERENCE: false
// EXPECTED_DATA_FLOW_COMPILE_TIME_VALUE: (1, 2)
// EXPECTED_DATA_FLOW_EXPRESSION_TYPE: (Int64, Int64)
package sample.dataflow

func useTuple() {
    let pair = (1, 2)
}
