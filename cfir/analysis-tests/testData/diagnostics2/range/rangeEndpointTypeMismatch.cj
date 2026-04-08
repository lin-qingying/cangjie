// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func startStopMismatch(): Unit {
    let broken = <!INCONSISTENCY_RANGE_ELEMENT_TYPE!>/* suggested message: range start and stop expressions must agree on element type. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/RangeExpr.cpp */0..true<!>
    println(broken)
}

func stringBoolMismatch(): Unit {
    let broken = <!INCONSISTENCY_RANGE_ELEMENT_TYPE!>/* suggested message: range start and stop expressions must agree on element type. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/RangeExpr.cpp */"a"..false<!>
    println(broken)
}
