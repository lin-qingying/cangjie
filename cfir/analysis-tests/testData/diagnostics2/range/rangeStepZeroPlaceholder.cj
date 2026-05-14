// RUN_PIPELINE_TILL: FRONTEND
func badRange(): Unit {
    let broken = 0..10 : <!RANGE_STEP_CANNOT_BE_ZERO!>0<!>
    let _ = broken
}
