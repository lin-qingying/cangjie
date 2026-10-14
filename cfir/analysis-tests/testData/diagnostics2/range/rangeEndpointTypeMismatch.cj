// RUN_PIPELINE_TILL: FRONTEND
func startStopMismatch(): Unit {
    let broken = 0..<!CANNOT_CONVERT_LITERAL!>true<!>
    let _ = broken
}

func stringBoolMismatch(): Unit {
    let broken = "a"..<!CANNOT_CONVERT_LITERAL!>false<!>
    let _ = broken
}
