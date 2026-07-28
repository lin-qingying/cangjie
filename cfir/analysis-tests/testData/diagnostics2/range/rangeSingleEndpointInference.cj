// RUN_PIPELINE_TILL: FRONTEND
func expectIntRange(r: Range<Int64>): Range<Int64> {
    return r
}

func boundedRangeMatchesContext(): Range<Int64> {
    return expectIntRange(0..10)
}

func boundedRangeBoolStartMismatch(): Range<Int64> {
    return  expectIntRange(<!TYPE_MISMATCH!>true<!>..10)
}
