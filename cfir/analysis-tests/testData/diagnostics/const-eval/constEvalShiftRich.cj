// RUN_PIPELINE_TILL: FRONTEND
func constEvalShiftDiagnostics(): Int64 {
    let negative: Int64 = <!CONST_EVAL_NEGATIVE_SHIFT_COUNT!>1 << -1<!>
    let overflow: Int64 = <!CONST_EVAL_SHIFT_COUNT_OVERFLOW!>1 << 64<!>
    let ok: Int64 = 8 >> 2
    return negative + overflow + ok
}
