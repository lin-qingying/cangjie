// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func stepBool(): Unit {
    let broken = <!RANGE_STEP_NOT_INT64!>/* suggested message: range step expression must be Int64. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/RangeExpr.cpp */0..10 : true<!>
    println(broken)
}

func stepFloat(): Unit {
    let broken = <!RANGE_STEP_NOT_INT64!>/* suggested message: range step expression must be Int64. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/RangeExpr.cpp */0..10 : 1.0<!>
    println(broken)
}
