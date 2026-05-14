// RUN_PIPELINE_TILL: FRONTEND
func expectIntRange(r: Range<Int64>): Range<Int64> {
    return r
}

func badTargetFromBoolStop(): Range<Int64> {
    return expectIntRange(0..<!TYPE_MISMATCH!>true<!>)
}

func goodTargetRange(): Range<Int64> {
    return expectIntRange(0..10)
}
