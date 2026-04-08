// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func oneSidedRangeOk(): Unit {
    let ok = 0.. : 1
    println(ok)
}

func oneSidedRangeBadStep(): Unit {
    let broken = <!RANGE_STEP_NOT_INT64!>/* suggested message: range step expression must be Int64 even for one-sided ranges. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/RangeExpr.cpp */0.. : false<!>
    println(broken)
}

func upperBoundOnlyBadStep(): Unit {
    let broken = <!RANGE_STEP_NOT_INT64!>/* suggested message: range step expression must be Int64 even when only the upper bound is present. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/RangeExpr.cpp */..10 : 1.0<!>
    println(broken)
}
