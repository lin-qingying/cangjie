// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func descendingPositiveStep(): Unit {
    let broken = <!RANGE_DIRECTION_STEP_MISMATCH!>/* suggested message: range step direction does not match the interval direction. source: manual_source_zh_cn_range; external/cangjie_compiler/src/Sema/TypeCheckExpr/RangeExpr.cpp */10..0 : 1<!>
    println(broken)
}

func ascendingNegativeStep(): Unit {
    let broken = <!RANGE_DIRECTION_STEP_MISMATCH!>/* suggested message: range step direction does not match the interval direction. source: manual_source_zh_cn_range; external/cangjie_compiler/src/Sema/TypeCheckExpr/RangeExpr.cpp */0..10 : -1<!>
    println(broken)
}
