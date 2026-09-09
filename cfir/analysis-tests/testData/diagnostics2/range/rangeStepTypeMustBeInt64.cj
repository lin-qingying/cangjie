// RUN_PIPELINE_TILL: FRONTEND
func stepBool(): Unit {
    let broken = 0..10 : <!CANNOT_CONVERT_LITERAL!>true<!>
    let _ = broken
}

func stepFloat(): Unit {
    let broken = 0..10 : <!CANNOT_CONVERT_LITERAL!>1.0<!>
    let _ = broken
}
