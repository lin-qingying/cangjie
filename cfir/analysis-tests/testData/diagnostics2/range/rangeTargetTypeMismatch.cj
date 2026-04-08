// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func expectIntRange(r: Range<Int64>): Range<Int64> {
    return r
}

func badTargetFromBoolStop(): Range<Int64> {
    return <!TYPE_MISMATCH!>/* suggested message: range elements must satisfy the target range element type. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/RangeExpr.cpp */expectIntRange(0..true)<!>
}

func goodTargetRange(): Range<Int64> {
    return expectIntRange(0..10)
}
