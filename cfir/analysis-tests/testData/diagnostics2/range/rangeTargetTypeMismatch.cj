// RUN_PIPELINE_TILL: FRONTEND
func expectIntRange(r: Range<Int64>): Range<Int64> {
    return r
}

func badTargetFromBoolStop(): Range<Int64> {
    return expectIntRange(0..<!CANNOT_CONVERT_LITERAL!>true<!>)
}

func goodTargetRange(): Range<Int64> {
    return expectIntRange(0..10)
}
