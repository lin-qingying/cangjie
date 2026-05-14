// RUN_PIPELINE_TILL: FRONTEND
func stepBool(): Unit {
    let broken = 0..10 : <!TYPE_MISMATCH!>true<!>
    let _ = broken
}

func stepFloat(): Unit {
    let broken = 0..10 : <!TYPE_MISMATCH!>1.0<!>
    let _ = broken
}
