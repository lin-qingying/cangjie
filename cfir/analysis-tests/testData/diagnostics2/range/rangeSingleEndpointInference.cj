// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func expectIntRange(r: Range<Int64>): Range<Int64> {
    return r
}

func stopOnlyInfersInt64(): Range<Int64> {
    return expectIntRange(..10)
}

func stopOnlyBoolMismatch(): Range<Int64> {
    return <!TYPE_MISMATCH!>/* suggested message: single-endpoint range must still satisfy the target element type after default inference. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/RangeExpr.cpp */expectIntRange(..true)<!>
}
