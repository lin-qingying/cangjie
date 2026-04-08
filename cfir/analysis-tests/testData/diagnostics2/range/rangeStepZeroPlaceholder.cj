// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func badRange(): Unit {
    let broken = <!RANGE_STEP_CANNOT_BE_ZERO!>/* suggested message: range step cannot be zero. source: manual_source_zh_cn_range; external/cangjie_compiler/src/Sema/TypeCheckExpr/RangeExpr.cpp */0..10 : 0<!>
    println(broken)
}
