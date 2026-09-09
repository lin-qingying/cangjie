// RUN_PIPELINE_TILL: FRONTEND
func startStopMismatch(): Unit {
    let broken = 0..<!CANNOT_CONVERT_LITERAL!>true<!>
    let _ = broken
}

func stringBoolMismatch(): Unit {
    let broken = "a"..<!TYPE_MISMATCH!>false<!>
    let _ = broken
}
